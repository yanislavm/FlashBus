package com.msagi.eventbus;

import android.os.Handler;
import android.util.Log;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles all the events that need to be fired on the Handler
 * @author yanislav.mihaylov
 */
public class MessagesHandler {
	/**
	 * Tag for logging.
	 */
	private static final String TAG = MessagesHandler.class.getSimpleName();

	/**
	 * Holds the different Events with their event handlers
	 */
	private static class EventsHolder {
		/**
		 * Event Handler
		 */
		private EventBus.IEventHandler mEventHandler;
		/**
		 * Event
		 */
		private EventBus.IEvent mEvent;

		/**
		 * Constructor
		 * @param event Event that needs to be triggered
		 * @param eventHandler Event Handler that will be notified for the event
		 */
		private EventsHolder(EventBus.IEvent event, EventBus.IEventHandler eventHandler) {
			mEvent = event;
			mEventHandler = eventHandler;
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
			while ((eventHolder= mEvents.poll()) != null) {
				try {
					eventHolder.mEventHandler.onEvent(eventHolder.mEvent);
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
	 * @param handler Handler on which we will handle the events
	 */
	protected MessagesHandler(final Handler handler) {
		mHandler = handler;
	}

	/**
	 * Posts events
	 * @param event Event that needs to be posted
	 * @param eventHandler Event handler that will handle the event
	 * @param <T> Event type
	 */
	public <T extends EventBus.IEvent> void postEvent(final T event, final EventBus.IEventHandler<T> eventHandler) {
		mEvents.add(new EventsHolder(event, eventHandler));

		if (mHandlerTriggered.compareAndSet(/* Expected value */ false, /* New Value*/ true)) {
			mHandler.post(mHandlerRunnable);
		}
	}
}
