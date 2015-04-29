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
 * - no inheritance in event types, events are delivered to their exact matching handlers
 * - optional unregistration, callbacks are kept as weak references and registry cleanup happens on every .post call
 *
 * @author msagi
 */
public class EventBus {

    /**
     * Target thread for event posting: UI (main) thread.
     */
    public static final byte UI_THREAD = 0;

    /**
     * Target thread for event posting: background thread.
     */
    public static final byte BACKGROUND_THREAD = 1;

    /**
     * Technical interface for event bus events.
     */
    public interface IEvent {}

    /**
     * Technical interface for event bus event handlers.
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
     * The singleton instance.
     */
    private static final EventBus DEFAULT_BUS = new EventBus();

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
     * Message handler that will be used to deliver events to the UI thread.
     */
    private final MessagesHandler mUiThreadMessageHandler;

    /**
     * Message handler that will be used to deliver events to the background thread.
     */
    private final MessagesHandler mBackgroundThreadMessageHandler;

    /**
     * Create new instance.
     */
    public EventBus() {
        mUiThreadMessageHandler = new MessagesHandler(new Handler(Looper.getMainLooper()));
        final HandlerThread backgroundHandlerThread = new HandlerThread("backgroundMessageHandler");
        backgroundHandlerThread.start();
        mBackgroundThreadMessageHandler = new MessagesHandler(new Handler(backgroundHandlerThread.getLooper()));
    }

    /**
     * Get the 'default' bus instance.
     *
     * @return The default bus instance.
     */
    public static EventBus getDefault() {
        return DEFAULT_BUS;
    }

    /**
     * Register a callback for the given event type. Multiple registration of the same handler has no effect.
     *
     * @param eventClass   The event type class.
     * @param eventHandler The callback of the event handler.
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

            for (final WeakReference<IEventHandler> handlerReference : handlers) {
                if (handlerReference.get() == eventHandler) {
                    return;
                }
            }

            handlers.add(new WeakReference<>(eventHandler));
        }
    }

    /**
     * Unregister a callback.
     *
     * @param eventHandler The callback of the event handler.
     */
    public void unregister(final IEventHandler eventHandler) {
        if (eventHandler == null) {
            return;
        }

        synchronized (mEventHandlers) {
            Class<? extends IEvent> eventClass = mEventHandlerEvent.get(eventHandler);

            if (eventClass != null) {
                mEventHandlerEvent.remove(eventHandler);
                final LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);
                if (handlers != null) {
                    final Iterator<WeakReference<IEventHandler>> handlerReferenceIterator = handlers.iterator();
                    while (handlerReferenceIterator.hasNext()) {
                        final WeakReference<IEventHandler> handlerReference = handlerReferenceIterator.next();
                        if ((handlerReference.get() == null) || (handlerReference.get() == eventHandler)) {
                            handlerReferenceIterator.remove();
                            if (handlers.isEmpty()) {
                                mEventHandlers.remove(eventClass);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the sticky event from the bus by event class.
     *
     * @param eventClass The class to get the sticky event for.
     * @return The sticky event object if there is any for the given event class.
     */
    public <T extends IEvent> T getStickyEvent(final Class<T> eventClass) {
        if (eventClass == null) {
            return null;
        }

        return eventClass.cast(mStickyEvents.get(eventClass));
    }

    /**
     * Post a sticky event to the bus.
     *
     * @param targetThread The target thread to post the sticky event to.
     * @param event The event to be posted.
     */
    public void postSticky(final int targetThread, final IEvent event) {
        if (event == null) {
            return;
        }

        mStickyEvents.put(event.getClass(), event);
        post(targetThread, event);
    }

    /**
     * Removes sticky event based on the event class
     * @param eventClass Event class for the sticky event that needs to be removed
     */
    public void removeSticky(final Class<? extends IEvent> eventClass) {
        if (eventClass == null) {
            return;
        }

        mStickyEvents.remove(eventClass);
    }

    /**
     * Post an event to the bus.
     *
     * @param targetThread The target thread to post the event to.
     * @param event The event.
     */
    public void post(final int targetThread, final IEvent event) {
        if (event == null) {
            return;
        }

        final Class<? extends IEvent> eventClass = event.getClass();
        final LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);

        if (handlers == null) {
            return;
        }

        final Iterator<WeakReference<IEventHandler>> handlersIterator = handlers.iterator();
        while (handlersIterator.hasNext()) {
            final IEventHandler handler = handlersIterator.next().get();
            if (handler == null) {
                //housekeeping of GCd callbacks
                handlersIterator.remove();
            } else {
                if (targetThread == UI_THREAD) {
                    mUiThreadMessageHandler.postEvent(event, handler);
                } else if (targetThread == BACKGROUND_THREAD) {
                    mBackgroundThreadMessageHandler.postEvent(event, handler);
                } else {
                    throw new IllegalArgumentException("unknown target thread");
                }
            }
        }
    }
}
