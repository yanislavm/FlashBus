package com.msagi.eventbus.app.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.msagi.eventbus.app.BenchmarkEvent;
import com.msagi.eventbus.app.IBenchmarkCallback;

/**
 * Base async task for benchmarks.
 *
 * @author msagi
 */
public abstract class BaseBenchmarkAsyncTask extends AsyncTask<Void, Void, Void> {

    /**
     * Tag for logging.
     */
    private static final String TAG = BaseBenchmarkAsyncTask.class.getSimpleName();

    /**
     * Time interval before posting.
     */
    private static final long SLEEP_BEFORE_POSTING = 2000L;

    /**
     * The array of events to post during the benchmark.
     */
    private final BenchmarkEvent[] mEvents;

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
    public BaseBenchmarkAsyncTask(final BenchmarkEvent[] events) {
        if (events == null) {
            throw new IllegalArgumentException("events == null");
        }
        if (events.length < 1) {
            throw new IllegalArgumentException("events.length < 1");
        }
        mEvents = events;
    }

    /**
     * Set benchmark callback interface.
     *
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
     * Post given event to the event bus.
     *
     * @param event The event to be posted to the event bus.
     */
    public abstract void post(final BenchmarkEvent event);

    /**
     * The event delivery registration method.
     *
     * @param event The event to register as delivered.
     */
    protected void onEventDelivered(final BenchmarkEvent event) {
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
            Thread.currentThread().sleep(SLEEP_BEFORE_POSTING);
        } catch (InterruptedException ie) {
            Log.e(TAG, "Interrupted", ie);
        }

        mPostingStartTimestamp = System.nanoTime();
        for (int index = 0; index < mEvents.length; index++) {
            post(mEvents[index]);
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
            if (mTotalEventsDelivered >= mEvents.length) {
                mEventDeliveryTotalTime = System.nanoTime() - mPostingStartTimestamp;
            }
            mBenchmarkCallback.onBenchmarkFinished(getClass().getSimpleName(), mFastestDeliveryTime, mSlowestDeliveryTime, mTotalEventsDelivered,
                    mEventDeliveryTotalTime);
        }
    }
}

