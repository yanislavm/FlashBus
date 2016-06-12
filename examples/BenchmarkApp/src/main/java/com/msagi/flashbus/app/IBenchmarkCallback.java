package com.msagi.flashbus.app;

/**
 * The benchmark callback interface.
 * @author msagi
 */
public interface IBenchmarkCallback {

    /**
     * Callback method on benchmark finished.
     * @param benchmarkId The id of the benchmark.
     * @param fastestDeliveryTime The fastest delivery time.
     * @param slowestDeliveryTime The slowest delay in nanoseconds.
     * @param numberOfDeliveredEvents The number of delivered events.
     * @param totalDeliveryTime The total benchmark time.
     */
    void onBenchmarkFinished(String benchmarkId, long fastestDeliveryTime, long slowestDeliveryTime, long numberOfDeliveredEvents, long totalDeliveryTime);
}
