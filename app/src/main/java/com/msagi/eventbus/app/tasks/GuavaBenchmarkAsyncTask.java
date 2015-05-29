package com.msagi.eventbus.app.tasks;

import android.os.Handler;
import android.os.Looper;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.msagi.eventbus.app.BenchmarkEvent;

import java.util.concurrent.Executor;

/**
 * Custom async task for Guava Event Bus.
 */
public class GuavaBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * The event bus instance.
     */
    private EventBus mGuavaBus;

    /**
     * Create new instance.
     *
     * @param events The array of events to post during benchmark.
     */
    public GuavaBenchmarkAsyncTask(final BenchmarkEvent[] events) {
        super(events);
    }

    @Override
    public void register() {
        mGuavaBus = new AsyncEventBus(new Executor() {
            private Handler mHandler;

            @Override
            public void execute(Runnable command) {
                if (mHandler == null) {
                    mHandler = new Handler(Looper.getMainLooper());
                }
                mHandler.post(command);
            }
        });
        mGuavaBus.register(this);
    }

    @Override
    public void unregister() {
        mGuavaBus.unregister(this);
    }

    @Override
    public void post(final BenchmarkEvent event) {
        event.resetLifeTime();
        mGuavaBus.post(event);
    }

    @com.google.common.eventbus.Subscribe
    public void onEvent(final BenchmarkEvent event) {
        super.onEventDelivered(event);
    }
}