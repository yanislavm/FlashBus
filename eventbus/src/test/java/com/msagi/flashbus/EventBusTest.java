package com.msagi.flashbus;

import com.msagi.flashbus.events.TestEvent;
import com.msagi.flashbus.events.TestEventWithData;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JUnit test for {@link EventBus}
 *
 * @author yanislav.mihaylov
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class EventBusTest {
    private final EventBus mEventBus = new EventBus();
    private final List<EventBus.IEvent> mReceivedEvents = new ArrayList<>();

    @Test
    public void getDefaultBus() {
        final EventBus bus = EventBus.getDefault();
        Assert.assertNotNull(bus);
    }

    @Test
    public void registrationWithNull() {
        mEventBus.register(null, EventBus.ThreadMode.MAIN, null);
    }

    @Test
    public void postNullEvent() {
        mEventBus.post(null);
    }

    @Test
    public void postNullStickyEvent() {
        mEventBus.postSticky(null);
    }

    @Test
    public void getStickyWithNullValue() {
        assertNull(mEventBus.getStickyEvent(null));
    }

    @Test
    public void unregisterWithNullEventHandler() {
        mEventBus.unregister(null);
    }

    @Test
    public void registerNewEventHandler() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.post(new TestEvent());

        runUiTasks();
        assertEquals(1, mReceivedEvents.size());
    }

    @Test
    public void duplicateEventHandlerRegistration() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.post(new TestEvent());

        runUiTasks();
        assertEquals(1, mReceivedEvents.size());
    }

    @Test
    public void registerNewEventHandlerAfterGarbageCollection() {
        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        });

        // We are triggering GC as the EventBus does keep a week reference to the event handlers.
        // This is done in order to prevent possible memory leaks. So after the GC as there is no
        // reference to the handler we have not to be able to find any events
        System.gc();

        mEventBus.post(new TestEvent());

        runUiTasks();
        assertEquals(0, mReceivedEvents.size());
    }

    @Test
    public void subscribeForEventButTriggerAnotherOne() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.post(new TestEventWithData("test"));

        runUiTasks();
        assertEquals(0, mReceivedEvents.size());
    }

    @Test
    public void correctEventDelivery() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        final EventBus.IEventHandler<TestEventWithData> eventHandler2 = new EventBus.IEventHandler<TestEventWithData>() {
            @Override
            public void onEvent(final TestEventWithData event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.register(TestEventWithData.class, EventBus.ThreadMode.MAIN, eventHandler2);
        mEventBus.post(new TestEventWithData("test"));

        runUiTasks();
        assertEquals(1, mReceivedEvents.size());
        final EventBus.IEvent receivedEvent = mReceivedEvents.get(0);
        assertEquals(TestEventWithData.class, receivedEvent.getClass());
        assertEquals("test", ((TestEventWithData) receivedEvent).getData());
    }

    @Test
    public void multipleEventsDelivery() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        final EventBus.IEventHandler<TestEventWithData> eventHandler2 = new EventBus.IEventHandler<TestEventWithData>() {
            @Override
            public void onEvent(final TestEventWithData event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.register(TestEventWithData.class, EventBus.ThreadMode.MAIN, eventHandler2);

        mEventBus.post(new TestEvent());
        mEventBus.post(new TestEventWithData("test"));

        runUiTasks();
        assertEquals(2, mReceivedEvents.size());
    }

    @Test
    public void missingStickyEvent() {
        final TestEvent event = mEventBus.getStickyEvent(TestEvent.class);
        assertNull(event);
    }

    @Test
    public void postStickyEvent() {
        mEventBus.postSticky(new TestEvent());
        final TestEvent event = mEventBus.getStickyEvent(TestEvent.class);
        assertNotNull(event);
    }

    @Test
    public void receiveStickyEvent() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.postSticky(new TestEvent());

        runUiTasks();
        assertEquals(1, mReceivedEvents.size());
        Assert.assertNotNull(mEventBus.getStickyEvent(TestEvent.class));
    }

    @Test
    public void receiveStickyEventOnRegistration() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.postSticky(new TestEvent());
        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);

        runUiTasks();
        assertEquals(1, mReceivedEvents.size());
        Assert.assertNotNull(mEventBus.getStickyEvent(TestEvent.class));
    }

    @Test
    public void removeStickyEventWithNull() {
        mEventBus.removeSticky(null);
    }

    @Test
    public void removeStickyEvent() {
        mEventBus.postSticky(new TestEvent());
        Assert.assertNotNull(mEventBus.getStickyEvent(TestEvent.class));
        mEventBus.removeSticky(TestEvent.class);
        Assert.assertNull(mEventBus.getStickyEvent(TestEvent.class));
    }

    @Test
    public void unregisterEventHandler() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                mReceivedEvents.add(event);
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.unregister(eventHandler);

        mEventBus.post(new TestEvent());

        runUiTasks();
        assertEquals(0, mReceivedEvents.size());
    }

    @Test
    public void unexpectedExceptionThrownFromEventHandler() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                throw new RuntimeException();
            }
        };

        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);
        mEventBus.post(new TestEvent());

        runUiTasks();
        assertEquals(0, mReceivedEvents.size());
    }

    @Test
    public void unexpectedExceptionThrownFromEventHandlerDuringRegistration() {
        final EventBus.IEventHandler<TestEvent> eventHandler = new EventBus.IEventHandler<TestEvent>() {
            @Override
            public void onEvent(final TestEvent event) {
                throw new RuntimeException();
            }
        };

        mEventBus.postSticky(new TestEvent());
        mEventBus.register(TestEvent.class, EventBus.ThreadMode.MAIN, eventHandler);

        runUiTasks();
        assertEquals(0, mReceivedEvents.size());
    }

    /**
     * Forces Robolectric to force Handler to deliver events to the MAIN thread
     */
    private void runUiTasks() {
        Robolectric.runUiThreadTasks();
    }

    /**
     * Forces Robolectric to force Handler to deliver events to the MAIN thread
     */
    private void runBackgroundTasks() {
        Robolectric.runUiThreadTasks();
    }
}
