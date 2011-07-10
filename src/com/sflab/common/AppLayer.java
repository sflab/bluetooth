package com.sflab.common;

public abstract class AppLayer<Datastore> {

	private boolean mIsActive = false;

	public boolean isActive() {
		return mIsActive;
	}

	final void open(Datastore datastore) {
		onOpen(datastore);
		mIsActive = true;
	}

	final void update(Datastore datastore) {
		onUpdate(datastore);
	}

	final void close() {
		onClose();
		mIsActive = false;
	}

	final void foreground() {
		if (!mIsActive) {
			onForeground();
			mIsActive = true;
		}
	}

	final void background() {
		if (mIsActive) {
			onBackground();
			mIsActive = false;
		}
	}

	final boolean back() {
		return onBack();
	}

	protected abstract void onOpen(Datastore datastore);
	protected abstract void onUpdate(Datastore datastore);
	protected abstract void onClose();
	protected abstract void onForeground();
	protected abstract void onBackground();
	protected abstract boolean onBack();
}
