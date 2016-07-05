package com.msagi.flashbus.app;

import com.flashbus.EventBus;

/**
 * Benchmark event.
 * @author msagi
 */
public class BenchmarkEvent implements EventBus.IEvent {

    /**
     * The timestamp of the event created.
     */
    private long mStartTimestamp;

    /**
     * Reset the life time counter.
     */
    public void resetLifeTime() {
        mStartTimestamp = System.nanoTime();
    }

    /**
     * Get the time elapsed since creating of this instance.
     *
     * @return The time in nanoseconds.
     */
    public long getLifetimeTime() {
        return System.nanoTime() - mStartTimestamp;
    }
}
