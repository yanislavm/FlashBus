package com.msagi.eventbus;

import android.os.Handler;
import android.util.Log;

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
    private static class EventsHolder {

        /**
         * The size of the holder cache.
         */
        private static final int CACHE_SIZE = 250;

        /**
         * Holder instance cache to recycle unused holders instead of creating and garbage collecting them.
         */
        private static final ConcurrentLinkedQueue<EventsHolder> CACHE = new ConcurrentLinkedQueue<>();

        /**
         * Event Handler to deliver the event to.
         */
        private EventBus.IEventHandler mEventHandler;

        /**
         * Event to deliver to the event handler.
         */
        private EventBus.IEvent mEvent;

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
            if (CACHE.size() < CACHE_SIZE) {
                mEvent = null;
                mEventHandler = null;
                CACHE.add(this);
            }
        }
    }

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
     * Dispatch event to its event handler.
     *
     * @param event        The event to be dispatched.
     * @param eventHandler The event handler the event to be dispatched to.
     * @param <T>          The type of the event.
     */
    public <T extends EventBus.IEvent> void dispatch(final T event, final EventBus.IEventHandler<T> eventHandler) {
        mEventQueue.add(EventsHolder.obtain(event, eventHandler));

        if (mIsEventDispatcherActive.compareAndSet(/* expected value */ false, /* new value */ true)) {
            mHandler.post(mEventDispatcherWorker);
        }
    }
}
