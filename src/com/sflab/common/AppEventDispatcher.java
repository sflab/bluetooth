package com.sflab.common;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import android.os.Handler;

public class AppEventDispatcher {

	private static class Receiver<T extends AppEvent> {
		//private final Class<T> mEventType;
		private final List<WeakReference<AppEventAction<T>>> mActions;

		Receiver(Class<T> eventType) {
			//mEventType = eventType;
			mActions = new ArrayList<WeakReference<AppEventAction<T>>>();
		}
		void send(T event) {
			boolean hasReleasedAction = false;
			for(WeakReference<AppEventAction<T>> actionRef : mActions) {
				AppEventAction<T> action = actionRef.get();
				if (action != null) {
					action.action(event);
				} else {
					hasReleasedAction = true;
				}
			}
			if (hasReleasedAction) {
				Iterator<WeakReference<AppEventAction<T>>> it = mActions.iterator();
				while(it.hasNext()) {
					WeakReference<AppEventAction<T>> ref = it.next();
					if (ref.isEnqueued()) {
						it.remove();
					}
				}
			}
		}
		void add(AppEventAction<T> action) {
			mActions.add(new WeakReference<AppEventAction<T>>(action));
		}
		void remove(AppEventAction<T> action) {
			Iterator<WeakReference<AppEventAction<T>>> it = mActions.iterator();
			while(it.hasNext()) {
				WeakReference<AppEventAction<T>> ref = it.next();
				if (action == ref.get()) {
					it.remove();
				}
			}
		}
	}

	private static class PostedEventProcessor {
		private final AppEventDispatcher mDispatcher;
		private final List<PostedEventInterface> mEntries;
		private final Handler mHandler;

		private interface PostedEventInterface {
			void send();
		}

		private class PostedEvent<T extends AppEvent> implements PostedEventInterface {
			private final T mEvent;

			public PostedEvent(T event) {
				mEvent = event;
			}
			public void send() {
				mDispatcher.send(mEvent);
			}
		}

		public PostedEventProcessor(AppEventDispatcher dispatcher) {
			mDispatcher = dispatcher;
			mEntries = new ArrayList<PostedEventInterface>();
			mHandler = new Handler();
		}

		public <Event extends AppEvent> void post(Event event) {
			if (push(event)) {
				mHandler.postAtFrontOfQueue(new Runnable() {
					public void run() {
						List<PostedEventInterface> events = pop();
						for(PostedEventInterface e : events) {
							e.send();
						}
					}
				});
			}
		}

		private <Event extends AppEvent> boolean push(Event event) {
			synchronized (mEntries) {
				boolean empty = mEntries.isEmpty();
				mEntries.add(new PostedEvent<Event>(event));
				return empty;
			}
		}

		private List<PostedEventInterface> pop() {
			List<PostedEventInterface> poped;
			synchronized (mEntries) {
				poped = new ArrayList<PostedEventInterface>(mEntries);
				mEntries.clear();
			}
			return poped;
		}
	};

	private final Map<
		Class<? extends AppEvent>,
		Receiver<? extends AppEvent>> mReceivers;

	private final PostedEventProcessor mPostedEventProcessor;

	public AppEventDispatcher() {
		mReceivers = new HashMap<
			Class<? extends AppEvent>,
			Receiver<? extends AppEvent>>();
		mPostedEventProcessor = new PostedEventProcessor(this);
	}

	public <Event extends AppEvent> void regist(AppEventAction<Event> action) {
		@SuppressWarnings("unchecked")
		Receiver<Event> receiver = (Receiver<Event>) mReceivers.get(action.getEventType());
		if (receiver == null) {
			receiver = new Receiver<Event>(action.getEventType());
			mReceivers.put(action.getEventType(), receiver);
		}
		receiver.add(action);
	}

	public <Event extends AppEvent> void unregist(AppEventAction<Event> action) {
		@SuppressWarnings("unchecked")
		Receiver<Event> receiver = (Receiver<Event>) mReceivers.get(action.getEventType());
		if (receiver != null) {
			receiver.remove(action);
		}
	}

	public <Event extends AppEvent> void send(Event event) {
		@SuppressWarnings("unchecked")
		Receiver<Event> receiver = (Receiver<Event>) mReceivers.get(event.getClass());
		if (receiver != null) {
			receiver.send(event);
		}
	}

	public <Event extends AppEvent> void post(Event event) {
		mPostedEventProcessor.post(event);
	}
}


