package com.msagi.eventbus.app.tasks;

import com.msagi.eventbus.app.event.GreenRobotEvent;

import de.greenrobot.event.EventBus;

/**
 * Custom async task for Green Robot.
 */
public class GreenRobotBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * The event bus instance.
     */
    private EventBus mGreenRobotBus;

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
    public void post() {
        mGreenRobotBus.post(new GreenRobotEvent());
    }

    public void onEventMainThread(final GreenRobotEvent event) {
        super.onEventDelivered(event);
    }
}