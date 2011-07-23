package com.sflab.common;

import java.util.Iterator;
import java.util.LinkedList;

public class AppLayerStack<Datastore> {

	private final LinkedList<AppLayer<Datastore>> mLayers;

	public AppLayerStack() {
		mLayers = new LinkedList<AppLayer<Datastore>>();
	}

	public boolean isEmpty() {
		return mLayers.isEmpty();
	}

	public void release() {
		Iterator<AppLayer<Datastore>> i = mLayers.iterator();
		while(i.hasNext()) {
			AppLayer<Datastore> layer = i.next();
			i.remove();
			layer.close();
		}
	}

	public void show(AppLayer<Datastore> layer, Datastore datastore) {
		if (!mLayers.isEmpty()) {
			AppLayer<Datastore> prev = mLayers.getLast();
			prev.background();
		}
		mLayers.addLast(layer);
		layer.open(datastore);
	}

	public void update(Datastore datastore) {
		for(AppLayer<Datastore> i : mLayers) {
			i.update(datastore);
		}
	}

	public boolean close(AppLayer<Datastore> layer) {
		if (mLayers.isEmpty()) {
			return false;
		}
		AppLayer<Datastore> prev = mLayers.getLast();
		if (mLayers.remove(layer)) {
			layer.close();
		}
		if (!mLayers.isEmpty()) {
			AppLayer<Datastore> next = mLayers.getLast();
			if (next != prev) {
				next.foreground();
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean back() {
		if (mLayers.isEmpty()) {
			// there is not layers.
			return false;
		}
		AppLayer<Datastore> prev = mLayers.getLast();
		if (prev.back()) {
			// The active layer handles a back key event.
			return true;
		}
		mLayers.remove(prev);
		prev.close();
		if (mLayers.isEmpty()) {
			// there is not layers.
			return false;
		}
		AppLayer<Datastore> next = mLayers.getLast();
		next.foreground();
		// change active layer.
		return true;
	}
}
