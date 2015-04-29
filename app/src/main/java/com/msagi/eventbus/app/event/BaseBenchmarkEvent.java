package com.msagi.eventbus.app.event;

/**
 * Base benchmark event.
 * @author msagi
 */
public abstract class BaseBenchmarkEvent {

    /**
     * The timestamp of the event created.
     */
    private final long mStartTimestamp;

    /**
     * Create new instance.
     */
    public BaseBenchmarkEvent() {
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
