package com.msagi.eventbus;

import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles all the events that need to be fired on the Handler
 *
 * @author yanislav.mihaylov
 */
public class MessagesHandler {
    /**
     * Tag for logging.
     */
    private static final String TAG = MessagesHandler.class.getSimpleName();

    /**
     * Holds Event - Event handler pairs.
     */
    private static class EventsHolder {

        /**
         * Cache size.
         */
        private static final int CACHE_SIZE = 250;

        /**
         * Cache to recycle unused events holders and lighten GC.
         */
        private static final ConcurrentLinkedQueue<EventsHolder> CACHE = new ConcurrentLinkedQueue<>();

        /**
         * Event Handler to deliver the event instance to.
         */
        private EventBus.IEventHandler mEventHandler;

        /**
         * Event instance to deliver.
         */
        private EventBus.IEvent mEvent;

        /**
         * Constructor
         *
         * @param event        Event that needs to be triggered
         * @param eventHandler Event Handler that will be notified for the event
         */
        private EventsHolder(final EventBus.IEvent event, final EventBus.IEventHandler eventHandler) {
            mEvent = event;
            mEventHandler = eventHandler;
        }

        /**
         * Obtain new holder instance.
         *
         * @param event        The event instance.
         * @param eventHandler The event handler instance.
         * @return The holder containing the given event - event handler pair.
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
         * Recycle holder instance.
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
     * The events that needs to be triggered on the Handler.
     */
    private ConcurrentLinkedQueue<EventsHolder> mEvents = new ConcurrentLinkedQueue<>();

    /**
     * Handler on which we will process the Events
     */
    private final Handler mHandler;

    /**
     * Instance of Runnable that will be used for the event delivery on the Handler
     */
    private final Runnable mHandlerRunnable = new Runnable() {
        @Override
        public void run() {
            EventsHolder eventHolder;
            while ((eventHolder = mEvents.poll()) != null) {
                try {
                    eventHolder.mEventHandler.onEvent(eventHolder.mEvent);
                    eventHolder.recycle();
                } catch (RuntimeException e) {
                    Log.e(TAG, "Error dispatching event", e);
                }
            }
            mHandlerTriggered.set(false);
        }
    };

    /**
     * Keeps the value if the Handler is triggered or not
     */
    private final AtomicBoolean mHandlerTriggered = new AtomicBoolean(false);

    /**
     * Constructor
     *
     * @param handler Handler on which we will handle the events
     */
    protected MessagesHandler(final Handler handler) {
        mHandler = handler;
    }

    /**
     * Posts events
     *
     * @param event        Event that needs to be posted
     * @param eventHandler Event handler that will handle the event
     * @param <T>          Event type
     */
    public <T extends EventBus.IEvent> void postEvent(final T event, final EventBus.IEventHandler<T> eventHandler) {
        mEvents.add(EventsHolder.obtain(event, eventHandler));

        if (mHandlerTriggered.compareAndSet(/* Expected value */ false, /* New Value*/ true)) {
            mHandler.post(mHandlerRunnable);
        }
    }
}
