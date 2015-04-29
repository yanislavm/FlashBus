package com.msagi.eventbus.app;

import com.msagi.eventbus.app.tasks.BaseBenchmarkAsyncTask;
import com.msagi.eventbus.app.tasks.FlashBenchmarkAsyncTask;
import com.msagi.eventbus.app.tasks.GreenRobotBenchmarkAsyncTask;
import com.msagi.eventbus.app.tasks.GuavaBenchmarkAsyncTask;
import com.msagi.eventbus.app.tasks.OttoBenchmarkAsyncTask;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.IOException;
import java.util.ArrayList;

/**
 * The benchmark activity.
 *
 * @author msagi
 */
public class BenchmarkActivity extends Activity implements IBenchmarkCallback {

    /**
     * The number of events per benchmark.
     */
    private static final long NUMBER_OF_EVENTS = 100000L;

    /**
     * Tag for logging.
     */
    private static final String TAG = BenchmarkActivity.class.getSimpleName();

    /**
     * Adapter for the list view.
     */
    private ArrayAdapter<String> mAdapter;

    /**
     * The list of messages.
     */
    private final ArrayList<String> mMessages = new ArrayList<>();

    /**
     * List of benchmark tasks.
     */
    private ArrayList<BaseBenchmarkAsyncTask> mBenchmarkTasks = new ArrayList<>();

    /**
     * Callback method for the benchmarks.
     *
     * @param benchmarkId             The id of the benchmark.
     * @param fastestDeliveryTime     The minimum delay.
     * @param slowestDeliveryTime     The maximum delay.
     * @param numberOfDeliveredEvents The number of delivered events.
     * @param totalDeliveryTime       The total benchmark time.
     */
    public void onBenchmarkFinished(final String benchmarkId, final long fastestDeliveryTime, final long slowestDeliveryTime, final long numberOfDeliveredEvents,
            final long totalDeliveryTime) {

        final String message;
        if (numberOfDeliveredEvents > 0) {
            message = String
                    .format("%s: %dk/%dk\nmin: %,d µs, max: %,d µs,\navg: %,d µs,\ntotal: %,d ms", benchmarkId, (numberOfDeliveredEvents / 1000),
                            (NUMBER_OF_EVENTS / 1000),
                            fastestDeliveryTime / 1000,
                            slowestDeliveryTime / 1000, (totalDeliveryTime / numberOfDeliveredEvents / 1000),
                            totalDeliveryTime / 1000000);
        } else {
            message = String.format("%s: no events delivered", benchmarkId);
        }

        mMessages.add(message);
        mAdapter.notifyDataSetChanged();

        try {
            StatisticPrinter.printStatistics(this, benchmarkId, numberOfDeliveredEvents, fastestDeliveryTime, slowestDeliveryTime, totalDeliveryTime);
        } catch (IOException ioe) {
            Log.e(TAG, "Cannot save statistics", ioe);
        }

        startNextBenchmark();
    }

    /**
     * Start next benchmark.
     */
    private void startNextBenchmark() {
        if (mBenchmarkTasks.isEmpty()) {
            final String message = "Benchmarks completed.";
            mMessages.add(message);
            mAdapter.notifyDataSetChanged();
            try {
                StatisticPrinter.printLineSeparator(this);
            } catch (IOException ioe) {
                Log.e(TAG, "Cannot save statistics", ioe);
            }

            return;
        }
        final BaseBenchmarkAsyncTask benchmarkAsyncTask = mBenchmarkTasks.remove(0);
        benchmarkAsyncTask.setNumberOfEvents(NUMBER_OF_EVENTS);
        benchmarkAsyncTask.setCallback(this);
        benchmarkAsyncTask.execute();
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_benchmark);
        mAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mMessages);
        final ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(mAdapter);

        final String message = "Starting benchmarks...";
        mMessages.clear();
        mMessages.add(message);
        mAdapter.notifyDataSetChanged();

        mBenchmarkTasks.add(new GuavaBenchmarkAsyncTask());
        mBenchmarkTasks.add(new FlashBenchmarkAsyncTask());
        mBenchmarkTasks.add(new GreenRobotBenchmarkAsyncTask());
        mBenchmarkTasks.add(new OttoBenchmarkAsyncTask());

        startNextBenchmark();
    }
}
