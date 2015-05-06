package com.msagi.eventbus.app.tasks;

import com.msagi.eventbus.app.event.OttoEvent;
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
    public void post() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mOttoBus.post(new OttoEvent());
            }
        });
    }

    @com.squareup.otto.Subscribe
    public void onEvent(final OttoEvent event) {
        super.onEventDelivered(event);
    }
}