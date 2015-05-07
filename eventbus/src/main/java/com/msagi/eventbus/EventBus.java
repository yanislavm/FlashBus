package com.msagi.eventbus;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.WeakHashMap;
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

    /**
     * Tag for logging.
     */
    private static final String TAG = EventBus.class.getSimpleName();

    /**
     * The singleton instance running on main (UI) thread.
     */
    private static EventBus sDefaultBus;

    /**
     * The event handlers registry.
     */
    private final ConcurrentHashMap<Class<? extends IEvent>, LinkedList<WeakReference<IEventHandler>>> mEventHandlers = new ConcurrentHashMap<>();

    /**
     * Keeps information which Event Handler for which Event type is.
     */
    private final WeakHashMap<IEventHandler, Class<? extends IEvent>> mEventHandlerEvent = new WeakHashMap<>();

    /**
     * The map of sticky events.
     */
    private final ConcurrentHashMap<Class<? extends IEvent>, IEvent> mStickyEvents = new ConcurrentHashMap<>();

    /**
     * The event dispatcher on the main thread.
     */
    private final EventDispatcher mMainEventDispatcher;

    /**
     * The event dispatcher on the background thread.
     */
    private final EventDispatcher mBackgroundEventDispatcher;

    /**
     * Create a new event bus instance. Use {@link #getDefault()} to get the default bus instance.
     */
    public EventBus() {
        mMainEventDispatcher = new EventDispatcher(new Handler(Looper.getMainLooper()));
        final HandlerThread backgroundHandlerThread = new HandlerThread("backgroundBus");
        backgroundHandlerThread.start();
        mBackgroundEventDispatcher = new EventDispatcher(new Handler(backgroundHandlerThread.getLooper()));
    }

    /**
     * Get the default event bus instance. Use this method only if you would like to use the default event bus. Use {@link #EventBus()} to create a new event bus instance.
     *
     * @return The bus instance running on the main (UI) thread.
     */
    public synchronized static EventBus getDefault() {
        if (sDefaultBus == null) {
            sDefaultBus = new EventBus();
        }
        return sDefaultBus;
    }

    /**
     * Register an event handler for the given event class. Registering a null event handler or multiple registration of the same handler has no effect.
     *
     * @param eventClass   The class of the event to register for.
     * @param eventHandler The event handler instance.
     */
    public void register(final Class<? extends IEvent> eventClass, final IEventHandler eventHandler) {
        if (eventHandler == null) {
            return;
        }

        synchronized (mEventHandlers) {
            mEventHandlerEvent.put(eventHandler, eventClass);
            LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);
            if (handlers == null) {
                handlers = new LinkedList<>();
                mEventHandlers.put(eventClass, handlers);
            }

            final IEvent stickyEvent = mStickyEvents.get(eventClass);
            if (stickyEvent != null) {
                try {
                    eventHandler.onEvent(eventClass.cast(stickyEvent));
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error dispatching event", e);
                }
            }

            final int handlerReferenceSize = handlers.size();
            for (int handlerReferenceIndex = 0; handlerReferenceIndex < handlerReferenceSize; handlerReferenceIndex++) {
                final WeakReference<IEventHandler> handlerReference = handlers.get(handlerReferenceIndex);
                if (handlerReference.get() == eventHandler) {
                    return;
                }
            }

            handlers.add(new WeakReference<>(eventHandler));
        }
    }

    /**
     * Unregister an already registered event handler. Unregistering null or not registered event handler has no effect.
     *
     * @param eventHandler The event handler instance to unregister.
     */
    public void unregister(final IEventHandler eventHandler) {
        if (eventHandler == null) {
            return;
        }

        synchronized (mEventHandlers) {
            final Class<? extends IEvent> eventClass = mEventHandlerEvent.get(eventHandler);

            if (eventClass != null) {
                mEventHandlerEvent.remove(eventHandler);
                final LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);

                if (handlers != null) {
                    int handlerReferenceSize = handlers.size();

                    for (int handlerReferenceIndex = 0; handlerReferenceIndex < handlerReferenceSize; ) {
                        final WeakReference<IEventHandler> handlerReference = handlers.get(handlerReferenceIndex);
                        final Object handler = handlerReference.get();
                        if (handler == null || handler == eventHandler) {
                            handlers.remove(handlerReferenceIndex);
                            handlerReferenceSize--;
                            if (handlerReferenceSize == 0) {
                                mEventHandlers.remove(eventClass);
                            }
                        } else {
                            handlerReferenceIndex++;
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the sticky event from the bus by event class.
     *
     * @param eventClass The event class to get the sticky event for.
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

        final Class<? extends IEvent> eventClass = event.getClass();
        final LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);

        if (handlers == null) {
            return;
        }
        int handlerReferenceSize = handlers.size();

        for (int handlerReferenceIndex = 0; handlerReferenceIndex < handlerReferenceSize; ) {
            final WeakReference<IEventHandler> handlerReference = handlers.get(handlerReferenceIndex);
            final IEventHandler handler = handlerReference.get();
            if (handler == null) {
                handlers.remove(handlerReferenceIndex);
                handlerReferenceSize--;
                if (handlerReferenceSize == 0) {
                    mEventHandlers.remove(eventClass);
                }
            } else {
                mMainEventDispatcher.dispatch(event, handler);
                handlerReferenceIndex++;
            }
        }
    }
}