package com.msagi.eventbus.app.tasks;

import com.msagi.eventbus.app.IBenchmarkCallback;
import com.msagi.eventbus.app.event.BaseBenchmarkEvent;

import android.os.AsyncTask;
import android.util.Log;

/**
 * Base async task for benchmarks.
 * @author msagi
 */
public abstract class BaseBenchmarkAsyncTask extends AsyncTask<Void, Void, Void> {

    /**
     * Tag for logging.
     */
    private static final String TAG = BaseBenchmarkAsyncTask.class.getSimpleName();

    /**
     * Number of events to be posted to the bus.
     */
    private long mNumberOfEvents = 1;

    /**
     * The minimum (fastest) delivery time.
     */
    private long mFastestDeliveryTime = Long.MAX_VALUE;

    /**
     * The maximum (slowest) delivery time.
     */
    private long mSlowestDeliveryTime = Long.MIN_VALUE;

    /**
     * The timestamp before the event posting starts to the event bus.
     */
    private long mPostingStartTimestamp = 0;

    /**
     * The sum of all delivery times.
     */
    private long mEventDeliveryTotalTime = 0;

    /**
     * The number of events delivered.
     */
    private long mTotalEventsDelivered = 0;

    /**
     * The callback interface.
     */
    private IBenchmarkCallback mBenchmarkCallback = null;

    /**
     * Create new instance.
     */
    public BaseBenchmarkAsyncTask() {
    }

    /**
     * Set number of events to post to the event bus during the benchmark.
     * @param numberOfEvents The number of events.
     */
    public void setNumberOfEvents(final long numberOfEvents) {
        if (numberOfEvents < 1) {
            throw new IllegalArgumentException("numberOfEvents < 1");
        }
        mNumberOfEvents = numberOfEvents;
    }

    /**
     * Set benchmark callback interface.
     * @param benchmarkCallback The callback interface.
     */
    public void setCallback(final IBenchmarkCallback benchmarkCallback) {
        mBenchmarkCallback = benchmarkCallback;
    }

    /**
     * Register to the event bus implementation.
     */
    public abstract void register();

    /**
     * Unregister from the event bus implementation.
     */
    public abstract void unregister();

    /**
     * Post new event to the event bus.
     */
    public abstract void post();

    /**
     * The event delivery registration method.
     * @param event The event to register as delivered.
     */
    protected void onEventDelivered(final BaseBenchmarkEvent event) {
        final long delta = event.getLifetimeTime();
        if (delta < mFastestDeliveryTime) {
            mFastestDeliveryTime = delta;
        }
        if (delta > mSlowestDeliveryTime) {
            mSlowestDeliveryTime = delta;
        }
        mTotalEventsDelivered++;
    }

    /**
     * Post the events to the event bus.
     */
    protected void postEvents() {
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException ie) {
            Log.e(TAG, "Interrupted", ie);
        }

        mPostingStartTimestamp = System.nanoTime();
        for (int index = 0; index < mNumberOfEvents; index++) {
            post();
        }
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        register();
    }

    @Override
    protected Void doInBackground(final Void... params) {
        postEvents();
        return null;
    }

    @Override
    protected void onPostExecute(final Void aVoid) {
        super.onPostExecute(aVoid);
        unregister();
        if (mBenchmarkCallback != null) {
            if (mTotalEventsDelivered >= mNumberOfEvents) {
                mEventDeliveryTotalTime = System.nanoTime() - mPostingStartTimestamp;
            }
            mBenchmarkCallback.onBenchmarkFinished(getClass().getSimpleName(), mFastestDeliveryTime, mSlowestDeliveryTime, mTotalEventsDelivered, mEventDeliveryTotalTime);
        }
    }
}

