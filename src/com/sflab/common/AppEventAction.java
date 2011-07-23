package com.sflab.common;

public abstract class AppEventAction<T extends AppEvent> {
	private final Class<T> mEventType;

	public AppEventAction(Class<T> eventType) {
		mEventType = eventType;
	}
	public Class<T> getEventType() {
		return mEventType;
	}
	protected abstract void action(T event);
}
