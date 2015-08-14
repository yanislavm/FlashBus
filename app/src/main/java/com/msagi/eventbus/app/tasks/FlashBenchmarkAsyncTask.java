package com.msagi.eventbus.app.tasks;

import android.util.Log;

import com.msagi.eventbus.EventBus;
import com.msagi.eventbus.EventDispatcher;
import com.msagi.eventbus.app.BenchmarkEvent;

/**
 * Custom async task for FlashBus.
 *
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
    private EventBus.IEventHandler<BenchmarkEvent> mEventHandler;

    /**
     * Create new instance.
     *
     * @param events The array of events to post during benchmark.
     */
    public FlashBenchmarkAsyncTask(final BenchmarkEvent[] events) {
        super(events);
    }

    @Override
    public void register() {
        mFlashBus = EventBus.getDefault();
        mEventHandler = new EventBus.IEventHandler<BenchmarkEvent>() {
            @Override
            public void onEvent(final BenchmarkEvent event) {
                onEventDelivered(event);
            }
        };
        mFlashBus.register(BenchmarkEvent.class, EventBus.THREAD_MAIN, mEventHandler);
    }

    @Override
    public void unregister() {
        mFlashBus.unregister(mEventHandler);
    }

    @Override
    public void post(final BenchmarkEvent event) {
        event.resetLifeTime();
        mFlashBus.post(event);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG, "Object pool size: " + EventDispatcher.EventsHolder.getCacheSize());
    }
}