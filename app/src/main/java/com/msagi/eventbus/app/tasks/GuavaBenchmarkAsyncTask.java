package com.msagi.eventbus.app.tasks;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;

import com.msagi.eventbus.app.event.GuavaEvent;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

/**
 * Custom async task for Guava Event Bus.
 */
public class GuavaBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * The event bus instance.
     */
    private EventBus mGuavaBus;

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
    public void post() {
        mGuavaBus.post(new GuavaEvent());
    }

    @com.google.common.eventbus.Subscribe
    public void onEvent(final GuavaEvent event) {
        super.onEventDelivered(event);
    }
}