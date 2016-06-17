package com.msagi.flashbus.app.tasks;

import com.msagi.flashbus.app.BenchmarkEvent;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;

import android.os.Handler;
import android.os.Looper;

/**
 * Custom async task for Otto.
 *
 * @author msagi
 */
public class OttoBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * The event bus instance.
     */
    private Bus mOttoBus;

    /**
     * Main handler.
     */
    private Handler mHandler = new Handler(Looper.getMainLooper());

    /**
     * Create new instance.
     *
     * @param events The array of events to post during benchmark.
     */
    public OttoBenchmarkAsyncTask(final BenchmarkEvent[] events) {
        super(events);
    }

    @Override
    public void register() {
        mOttoBus = new Bus(ThreadEnforcer.MAIN);
        mOttoBus.register(this);
    }

    @Override
    public void unregister() {
        mOttoBus.unregister(this);
    }

    @Override
    public void post(final BenchmarkEvent event) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                mOttoBus.post(event);
            }
        });
    }

    @com.squareup.otto.Subscribe
    public void onEvent(final BenchmarkEvent event) {
        super.onEventDelivered(event);
    }
}