package com.msagi.eventbus.app.tasks;

import android.util.Log;

import com.msagi.eventbus.EventBus;

import com.msagi.eventbus.EventDispatcher;
import com.msagi.eventbus.app.event.FlashBusEvent;

/**
 * Custom async task for FlashBus.
 * @author msagi
 */
public class FlashBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * Tag for logging.
     */
    private static final String TAG = FlashBenchmarkAsyncTask.class.getSimpleName();

    /**
     * The bus instance.
     */
    private EventBus mFlashBus;

    /**
     * The event handler.
     */
    private EventBus.IEventHandler<FlashBusEvent> mEventHandler;

    @Override
    public void register() {
        mFlashBus = EventBus.getDefault();
        mEventHandler = new EventBus.IEventHandler<FlashBusEvent>() {
            @Override
            public void onEvent(final FlashBusEvent event) {
                onEventDelivered(event);
            }
        };
        mFlashBus.register(FlashBusEvent.class, EventBus.ThreadId.MAIN, mEventHandler);
    }

    @Override
    public void unregister() {
        mFlashBus.unregister(mEventHandler);
    }

    @Override
    public void post() {
        mFlashBus.post(new FlashBusEvent());
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG, "Object pool size: " + EventDispatcher.EventsHolder.getCacheSize());
    }
}