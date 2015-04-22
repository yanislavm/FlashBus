package com.msagi.eventbus;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
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
     * Technical interface for event bus events.
     */
    public interface IEvent {}

    /**
     * Technical interface for event bus event handlers.
     */
    public interface IEventHandler {
        /**
         * Event handler.
         *
         * @param event The event instance.
         */
        void onEvent(IEvent event);
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
     * A handy handler on the UI thread.
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * The event handlers registry.
     */
    private final HashMap<Class<? extends IEvent>, LinkedList<WeakReference<IEventHandler>>> mEventHandlers = new HashMap<>();

    /**
     * The map of sticky events.
     */
    private final Map<Class<?>, Object> mStickyEvents = new ConcurrentHashMap<>();

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
            LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);
            if (handlers == null) {
                handlers = new LinkedList<>();
                mEventHandlers.put(eventClass, handlers);
            }
            boolean contains = false;
            for (final WeakReference<IEventHandler> handlerReference : handlers) {
                if (handlerReference.get() == eventHandler) {
                    contains = true;
                    break;
                }
            }

            if (!contains) {
                handlers.add(new WeakReference<>(eventHandler));
            }
        }
    }

    /**
     * Unregister a callback.
     *
     * @param eventHandler The callback of the event handler.
     */
    public void unregister(final Class<? extends IEvent> eventClass, final IEventHandler eventHandler) {
        if (eventHandler == null) {
            return;
        }
        synchronized (mEventHandlers) {
            final LinkedList<WeakReference<IEventHandler>> handlers = mEventHandlers.get(eventClass);
            if (handlers != null) {
                final Iterator<WeakReference<IEventHandler>> handlerReferenceIterator = handlers.iterator();
                while (handlerReferenceIterator.hasNext()) {
                    if (handlerReferenceIterator.next().get() == eventHandler) {
                        handlerReferenceIterator.remove();
                        return;
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
    public <T> T getStickyEvent(final Class<T> eventClass) {
        if (eventClass == null) {
            return null;
        }
        return eventClass.cast(mStickyEvents.get(eventClass));
    }

    /**
     * Post a sticky event to the bus.
     *
     * @param event The event to be posted.
     */
    public void postSticky(final IEvent event) {
        if (event == null) {
            return;
        }
        mStickyEvents.put(event.getClass(), event);
        post(event);
    }

    /**
     * Post an event to the bus.
     *
     * @param event The event.
     */
    public void post(final IEvent event) {
        if (event == null) {
            return;
        }
        final Class<? extends IEvent> eventClass = event.getClass();
        final LinkedList<WeakReference<IEventHandler>> handlers;
        synchronized (mEventHandlers) {
            handlers = mEventHandlers.get(eventClass);
        }
        if (handlers == null) {
            return;
        }
        final Iterator<WeakReference<IEventHandler>> handlersIterator = handlers.iterator();
        while (handlersIterator.hasNext()) {
            try {
                final IEventHandler handler = handlersIterator.next().get();
                if (handler == null) {
                    //housekeeping of GCd callbacks
                    handlersIterator.remove();
                } else {
                    mHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                handler.onEvent(event);
                            } catch (RuntimeException e) {
                                Log.e(TAG, "Error dispatching event", e);
                            }
                        }
                    });
                }
            } catch (RuntimeException e) {
                Log.e(TAG, "Error dispatching event", e);
            }
        }
    }
}
