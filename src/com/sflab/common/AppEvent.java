package com.sflab.common;

public class AppEvent {
	private final String mName;

	public AppEvent() {
		mName = getClass().getSimpleName();
	}

	public AppEvent(String name) {
		mName = name;
	}

	public String getName() {
		return mName;
	}
}
