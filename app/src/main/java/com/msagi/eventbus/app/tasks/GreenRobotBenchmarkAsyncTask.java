package com.msagi.eventbus.app.tasks;

import com.msagi.eventbus.app.BenchmarkEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Custom async task for Green Robot.
 */
public class GreenRobotBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * The event bus instance.
     */
    private EventBus mGreenRobotBus;

    /**
     * Create new instance.
     *
     * @param events The array of events to post during benchmark.
     */
    public GreenRobotBenchmarkAsyncTask(final BenchmarkEvent[] events) {
        super(events);
    }

    @Override
    public void register() {
        mGreenRobotBus = EventBus.getDefault();
        mGreenRobotBus.register(this);
    }

    @Override
    public void unregister() {
        mGreenRobotBus.unregister(this);
    }

    @Override
    public void post(final BenchmarkEvent event) {
        event.resetLifeTime();
        mGreenRobotBus.post(event);
    }

    @Subscribe (threadMode = ThreadMode.MAIN)
    public void onEventMainThread(final BenchmarkEvent event) {
        super.onEventDelivered(event);
    }
}