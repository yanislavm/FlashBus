package com.msagi.eventbus.app.tasks;

import com.msagi.eventbus.EventBus;

import com.msagi.eventbus.app.event.FlashBusEvent;

/**
 * Custom async task for FlashBus.
 * @author msagi
 */
public class FlashBenchmarkAsyncTask extends BaseBenchmarkAsyncTask {

    /**
     * The bus instance.
     */
    private EventBus mFlashBus;

    /**
     * The event handler.
     */
    private EventBus.IEventHandler<FlashBusEvent> mEventHandler;

    @Override
    public void register() {
        mFlashBus = EventBus.getDefault();
        mEventHandler = new EventBus.IEventHandler<FlashBusEvent>() {
            @Override
            public void onEvent(final FlashBusEvent event) {
                onEventDelivered(event);
            }
        };
        mFlashBus.register(FlashBusEvent.class, mEventHandler);
    }

    @Override
    public void unregister() {
        mFlashBus.unregister(mEventHandler);
    }

    @Override
    public void post() {
        mFlashBus.post(EventBus.UI_THREAD, new FlashBusEvent());
    }
}