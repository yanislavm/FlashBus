package com.msagi.flashbus;

import android.os.Handler;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dispatches events to their event handlers.
 *
 * @author msagi (miklos.sagi@gmail.com)
 * @author yanislav.mihaylov (jany81@gmail.com)
 */
public class EventDispatcher {
    /**
     * Tag for logging.
     */
    private static final String TAG = EventDispatcher.class.getSimpleName();

    /**
     * Holder for Event - Event handler pairs.
     */
    public static class EventsHolder {

        /**
         * The size of the holder cache.
         */
        private static final int CACHE_SIZE = 1000;

        /**
         * Holder instance cache to recycle unused holders instead of creating and garbage collecting them.
         */
        private static final ConcurrentLinkedQueue<EventsHolder> CACHE;

        /**
         * Event Handler to deliver the event to.
         */
        private EventBus.IEventHandler mEventHandler;

        /**
         * Event to deliver to the event handler.
         */
        private EventBus.IEvent mEvent;

        static {
            CACHE = new ConcurrentLinkedQueue<>();
            //pre-fill object pool in one loop
            for (int pooledObjectIndex = 0; pooledObjectIndex < CACHE_SIZE; pooledObjectIndex++) {
                CACHE.add(new EventsHolder(null, null));
            }
        }

        public static final int getCacheSize() {
            return CACHE.size();
        }

        /**
         * Create new instance.
         *
         * @param event        The event to be dispatched.
         * @param eventHandler The event handler the event to be dispatched to.
         */
        private EventsHolder(final EventBus.IEvent event, final EventBus.IEventHandler eventHandler) {
            mEvent = event;
            mEventHandler = eventHandler;
        }

        /**
         * Obtain new holder instance (tries to re-use holders from cache first).
         *
         * @param event        The event to be dispatched.
         * @param eventHandler The event handler the event to be dispatched to.
         * @return The holder set up using the event and event handler.
         */
        private static EventsHolder obtain(final EventBus.IEvent event, final EventBus.IEventHandler eventHandler) {
            final EventsHolder cachedHolder = CACHE.poll();
            if (cachedHolder == null) {
                return new EventsHolder(event, eventHandler);
            } else {
                cachedHolder.mEvent = event;
                cachedHolder.mEventHandler = eventHandler;
                return cachedHolder;
            }
        }

        /**
         * Recycle unused holder instance.
         */
        private void recycle() {
            //TODO msagi: uprade this to automatically find balanced object pool size
            mEvent = null;
            mEventHandler = null;
            CACHE.add(this);
        }
    }

    /**
     * The map of event handlers registered to this dispatcher.
     */
    private final ConcurrentHashMap<Class<? extends EventBus.IEvent>, LinkedList<WeakReference<EventBus.IEventHandler>>> mEventHandlers = new ConcurrentHashMap<>();

    /**
     * Keeps information which Event Handler for which Event type is.
     */
    private final WeakHashMap<EventBus.IEventHandler, Class<? extends EventBus.IEvent>> mEventHandlerEvent = new WeakHashMap<>();

    /**
     * The queue of event holders.
     */
    private final ConcurrentLinkedQueue<EventsHolder> mEventQueue = new ConcurrentLinkedQueue<>();

    /**
     * The {@link Handler} which {@link android.os.Looper} the event is dispatched on.
     */
    private final Handler mHandler;

    /**
     * The (thread safe) flag to keep track if dispatching is active or idle.
     */
    private final AtomicBoolean mIsEventDispatcherActive = new AtomicBoolean(false);

    /**
     * The worker to dispatch the events to the event listeners.
     */
    private final Runnable mEventDispatcherWorker = new Runnable() {
        @Override
        public void run() {
            EventsHolder eventHolder;
            while ((eventHolder = mEventQueue.poll()) != null) {
                try {
                    eventHolder.mEventHandler.onEvent(eventHolder.mEvent);
                    eventHolder.recycle();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error dispatching event", e);
                }
            }
            mIsEventDispatcherActive.set(false);
        }
    };

    /**
     * Create new instance.
     *
     * @param handler The {@link Handler} which {@link android.os.Looper} the events are dispatched on.
     */
    /* package */ EventDispatcher(final Handler handler) {
        mHandler = handler;
    }

    /**
     * Register an event handler for the given event class. Registering a null event handler or multiple registration of the same handler has no effect.
     *
     * @param eventClass   The class of the event to register for.
     * @param eventHandler The event handler instance.
     * @param stickyEvent  The sticky event instance to be dispatched on registration
     */
    public void register(final Class<? extends EventBus.IEvent> eventClass, final EventBus.IEventHandler eventHandler, final EventBus.IEvent stickyEvent) {

        synchronized (mEventHandlers) {
            mEventHandlerEvent.put(eventHandler, eventClass);
            LinkedList<WeakReference<EventBus.IEventHandler>> handlers = mEventHandlers.get(eventClass);
            if (handlers == null) {
                handlers = new LinkedList<>();
                mEventHandlers.put(eventClass, handlers);
            }

            if (stickyEvent != null) {
                try {
                    //this dispatch is synchronous but since an event handler cannot be registered twice, it cannot cause problem
                    eventHandler.onEvent(eventClass.cast(stickyEvent));
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error dispatching event", e);
                }
            }

            final int handlerReferenceSize = handlers.size();
            for (int handlerReferenceIndex = 0; handlerReferenceIndex < handlerReferenceSize; handlerReferenceIndex++) {
                final WeakReference<EventBus.IEventHandler> handlerReference = handlers.get(handlerReferenceIndex);
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
    public void unregister(final EventBus.IEventHandler eventHandler) {
        if (eventHandler == null) {
            return;
        }

        synchronized (mEventHandlers) {
            final Class<? extends EventBus.IEvent> eventClass = mEventHandlerEvent.get(eventHandler);

            if (eventClass != null) {
                mEventHandlerEvent.remove(eventHandler);
                final LinkedList<WeakReference<EventBus.IEventHandler>> handlers = mEventHandlers.get(eventClass);

                if (handlers != null) {
                    int handlerReferenceSize = handlers.size();

                    for (int handlerReferenceIndex = 0; handlerReferenceIndex < handlerReferenceSize; ) {
                        final WeakReference<EventBus.IEventHandler> handlerReference = handlers.get(handlerReferenceIndex);
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
     * Check if the event handler has already been registered to this dispatcher. Note: it is not allowed for an event listener to be registered on multiple dispatchers.
     *
     * @param eventClass   The class of the event to register for.
     * @param eventHandler The event handler instance.
     * @return True if the given event handler has been registered to the given event class on the current dispatcher, false otherwise.
     */
    boolean isRegistered(final Class<? extends EventBus.IEvent> eventClass, final EventBus.IEventHandler eventHandler) {
        final LinkedList<WeakReference<EventBus.IEventHandler>> handlers = mEventHandlers.get(eventClass);
        if (handlers != null) {
            return handlers.contains(eventHandler);
        }
        return false;
    }


    /**
     * Dispatch event to its event handler.
     *
     * @param event The event to be dispatched.
     * @param <T>   The type of the event.
     */
    public <T extends EventBus.IEvent> void dispatch(final T event) {

        final Class<? extends EventBus.IEvent> eventClass = event.getClass();
        final LinkedList<WeakReference<EventBus.IEventHandler>> handlers = mEventHandlers.get(eventClass);

        if (handlers == null) {
            return;
        }
        int handlerReferenceSize = handlers.size();

        for (int handlerReferenceIndex = 0; handlerReferenceIndex < handlerReferenceSize; ) {
            final WeakReference<EventBus.IEventHandler> handlerReference = handlers.get(handlerReferenceIndex);
            final EventBus.IEventHandler handler = handlerReference.get();
            if (handler == null) {
                handlers.remove(handlerReferenceIndex);
                handlerReferenceSize--;
                if (handlerReferenceSize == 0) {
                    mEventHandlers.remove(eventClass);
                }
            } else {
                mEventQueue.add(EventsHolder.obtain(event, handler));

                if (mIsEventDispatcherActive.compareAndSet(/* expected value */ false, /* new value */ true)) {
                    mHandler.post(mEventDispatcherWorker);
                }
                handlerReferenceIndex++;
            }
        }
    }
}
