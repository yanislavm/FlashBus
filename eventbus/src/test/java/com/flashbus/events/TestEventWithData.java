package com.flashbus.events;

import com.flashbus.EventBus;

/**
 * Test event that contains also a data
 * @author yanislav.mihaylov
 */
public class TestEventWithData implements EventBus.IEvent {
	/** Test data field */
	private String mData;

	/**
	 * Constructor
	 * @param data Test data
	 */
	public TestEventWithData(final String data) {
		mData = data;
	}

	/**
	 * Gets the Test Data
	 * @return Test Data
	 */
	public String getData() {
		return mData;
	}
}
