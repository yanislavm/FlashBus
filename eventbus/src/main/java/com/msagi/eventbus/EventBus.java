package com.msagi.eventbus;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Custom event bus implementation designed to be super fast.
 *
 * Features:
 * - Designed for speed.
 * - Supports all Android frameworks (API1+).
 * - Simple, clean, thread safe API.
 * - Unit tested with 100% coverage.
 * - No chance to leak context (the bus keeps weak references to the event handlers, this makes calling .unregister() optional).
 * - No inheritance in event types, events are delivered to their exact matching handlers.
 *
 * @author msagi (miklos.sagi@gmail.com)
 * @author yanislav.mihaylov (jany81@gmail.com)
 */
public class EventBus {

    /**
     * Interface for event bus events.
     */
    public interface IEvent {
    }

    /**
     * Interface for event bus event handlers.
     */
    public interface IEventHandler<E extends IEvent> {
        /**
         * Event handler.
         *
         * @param event The event instance.
         */
        void onEvent(E event);
    }

    @IntDef({ThreadMode.MAIN, ThreadMode.BACKGROUND})
    @Retention(RetentionPolicy.SOURCE)
    public @interface ThreadMode {
        /**
         * The Id of the MAIN thread.
         */
        int MAIN = 0;
        /**
         * The Id of the background thread.
         */
        int BACKGROUND = 1;
    }

    /**
     * The singleton instance running on main (MAIN) thread.
     */
    private static EventBus sDefaultBus;

    /**
     * The map of sticky events.
     */
    private final ConcurrentHashMap<Class<? extends IEvent>, IEvent> mStickyEvents = new ConcurrentHashMap<>();

    /**
     * The event dispatcher on the MAIN thread.
     */
    private final EventDispatcher mMainThreadEventDispatcher;

    /**
     * The event dispatcher on the background thread.
     */
    private final EventDispatcher mBackgroundThreadEventDispatcher;

    /**
     * Create a new event bus instance. Use {@link #getDefault()} to get the default bus instance.
     */
    public EventBus() {
        mMainThreadEventDispatcher = new EventDispatcher(new Handler(Looper.getMainLooper()));
        final HandlerThread backgroundHandlerThread = new HandlerThread("backgroundDispatcherThread");
        backgroundHandlerThread.start();
        mBackgroundThreadEventDispatcher = new EventDispatcher(new Handler(backgroundHandlerThread.getLooper()));
    }

    /**
     * Get the default event bus instance. Use this method only if you would like to use the default event bus. Use {@link #EventBus()} to create a new event bus instance.
     *
     * @return The bus instance running on the main (MAIN) thread.
     */
    public synchronized static EventBus getDefault() {
        if (sDefaultBus == null) {
            sDefaultBus = new EventBus();
        }
        return sDefaultBus;
    }

    /**
     * Register an event handler for the given event class. Registering to a null event class, registering a null event handler or multiple registration of the same
     * handler has no effect.
     *
     * @param eventClass   The class of the event to register for.
     * @param threadId     The id of the thread to dispatch the events on.
     * @param eventHandler The event handler instance.
     * @throws IllegalArgumentException If the event handler is already registered to the same event class on a different thread.
     */
    public void register(final Class<? extends IEvent> eventClass, @ThreadMode final int threadId, final IEventHandler eventHandler) {
        if (threadId < 0 || threadId > 1) {
            throw new IllegalArgumentException("Invalid threadId value");
        }
        if (eventHandler == null || eventClass == null) {
            return;
        }

        switch (threadId) {

            case ThreadMode.MAIN:
                if (mBackgroundThreadEventDispatcher.isRegistered(eventClass, eventHandler)) {
                    throw new IllegalArgumentException("Event handler has already been registered on BACKGROUND thread");
                }
                mMainThreadEventDispatcher.register(eventClass, eventHandler, mStickyEvents.get(eventClass));
                break;
            case ThreadMode.BACKGROUND:
                if (mMainThreadEventDispatcher.isRegistered(eventClass, eventHandler)) {
                    throw new IllegalArgumentException("Event handler has already been registered on MAIN thread");
                }
                mBackgroundThreadEventDispatcher.register(eventClass, eventHandler, mStickyEvents.get(eventClass));
                break;
        }
    }

    /**
     * Unregister an already registered event handler. Unregistering null or not registered event handler has no effect.
     *
     * @param eventHandler The event handler instance to unregister.
     */
    public void unregister(final EventBus.IEventHandler eventHandler) {
        mMainThreadEventDispatcher.unregister(eventHandler);
        mBackgroundThreadEventDispatcher.unregister(eventHandler);
    }

    /**
     * Get the sticky event from the bus by event class.
     *
     * @param eventClass The event class to get the sticky event for.
     * @param <T> Event class
     * @return The sticky event object for the given event class if any, null otherwise.
     */
    public <T extends IEvent> T getStickyEvent(final Class<T> eventClass) {
        if (eventClass == null) {
            return null;
        }
        return eventClass.cast(mStickyEvents.get(eventClass));
    }

    /**
     * Post a sticky event to the bus. The bus keeps hard reference to the event instance until it is removed (see {@link #removeSticky(Class)}) so take care of this
     * to avoid context leaking.
     *
     * @param event The event to be sticky posted.
     */
    public void postSticky(final IEvent event) {
        if (event == null) {
            return;
        }

        mStickyEvents.put(event.getClass(), event);
        post(event);
    }

    /**
     * Removes a previously posted sticky event by event class. Removing a null event class or an event class which instance has not been sticky posted has no effect.
     *
     * @param eventClass Event class for the sticky event to be removed.
     */
    public void removeSticky(final Class<? extends IEvent> eventClass) {
        if (eventClass == null) {
            return;
        }

        mStickyEvents.remove(eventClass);
    }

    /**
     * Post an event to the bus. Posting null event or posting an event which has no registered event handler has no effect. The events are delivered to the event
     * handlers in a 'first registered, first to receive' order.
     *
     * @param event The event to be posted.
     */
    public void post(final IEvent event) {
        if (event == null) {
            return;
        }

        mMainThreadEventDispatcher.dispatch(event);
        mBackgroundThreadEventDispatcher.dispatch(event);
    }
}