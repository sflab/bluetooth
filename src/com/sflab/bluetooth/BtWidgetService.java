package com.sflab.bluetooth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sflab.bluetooth.AppConfigure.HasAppConfigure;
import com.sflab.bluetooth.AppConfigure.NoPreferenceFoundError;
import com.sflab.bluetooth.AppConfigure.WidgetConfigure;
import com.sflab.bluetooth.Constants.Profile;
import com.sflab.bluetooth.connection.A2dpConnection;
import com.sflab.bluetooth.connection.BtConnection;
import com.sflab.bluetooth.connection.HeadsetConnection;
import com.sflab.bluetooth.connection.HeadsetConnection.ServiceListener;
import com.sflab.common.AppBroadcastReceiver;
import com.sflab.common.AppLogger;
import com.sflab.common.AppBroadcastReceiver.ActionCommand;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.widget.RemoteViews;
import android.widget.Toast;

public class BtWidgetService extends Service
implements HasAppConfigure, ServiceListener {

	private static final AppLogger LOG = Constants.LOGGER.get(BtWidgetService.class);

	private static final String ACTION_UPDATE = "com.sflab.bluetooth.ACTION_UPDATE";
	private static final String ACTION_DELETE = "com.sflab.bluetooth.ACTION_DELETE";
	static final String ACTION_SELECT = "com.sflab.bluetooth.ACTION_SELECT";
	static final String ACTION_ENABLE = "com.sflab.bluetooth.ACTION_ENABLE";
	static final String ACTION_RESUME = "com.sflab.bluetooth.ACTION_RESUME";

	private class ProfileEntry {
		private final BtConnection connection;
		private final List<String> currentDevices;

		ProfileEntry(BtConnection connection) {
			this.connection = connection;
			this.currentDevices = new ArrayList<String>();
		}

		void release() {
			this.connection.release();
			this.currentDevices.clear();
		}
		
		void connect(BluetoothDevice device) {
			LOG.ENTER(
					"name:"+device.getName(),
					"address:"+device.getAddress());
			if (device == null) {
				Toast.makeText(
						getBaseContext(),
						"can't connect the device",
						Toast.LENGTH_SHORT
				).show();
			} else {
				if (!this.connection.connect(device)) {
					Toast.makeText(
							getBaseContext(),
							"can't connect "+device.getName(),
							Toast.LENGTH_SHORT
					).show();
				} else {
					Toast.makeText(
							getBaseContext(),
							"connecting "+device.getName(),
							Toast.LENGTH_SHORT
					).show();
				}
			}
		}

		void disconnect(BluetoothDevice device) {
			LOG.ENTER(
					"name:"+device.getName(),
					"address:"+device.getAddress());
			if (device == null) {
				Toast.makeText(
						getBaseContext(),
						"can't disconnect the devie",
						Toast.LENGTH_SHORT
				).show();
			} else {
				if (!this.connection.disconnect(device)) {
					Toast.makeText(
							getBaseContext(),
							"can't disconnect "+device.getName(),
							Toast.LENGTH_SHORT
					).show();
				} else {
					Toast.makeText(
							getBaseContext(),
							"disconnecting "+device.getName(),
							Toast.LENGTH_SHORT
					).show();
				}
			}
		}

		void disconnect() {
			LOG.ENTER();
			for(BluetoothDevice device : this.connection.getConnectedDevices()) {
				connection.disconnect(device);
			}
		}

		void updateCurrentDevices() {
			this.currentDevices.clear();
			for(BluetoothDevice device : this.connection.getConnectedDevices()) {
				currentDevices.add(device.getAddress());
				LOG.DEBUG("  state:%s(%s)", device.getName(), device.getAddress());
			}
		}
	}

	private final Map<Profile, ProfileEntry> profiles;
	private final List<Widget> widgets;
	private BtState btState;
	private int requestId;

	public BtWidgetService() {
		profiles = new HashMap<Profile, ProfileEntry>();
		widgets = new ArrayList<Widget>();
		btState = BtState.Off;
		requestId = AppWidgetManager.INVALID_APPWIDGET_ID;
	}

	@Override
	public AppConfigure getAppConfigure() {
		return (AppConfigure) getApplication();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		LOG.ENTER(intent, startId);
		super.onStart(intent, startId);
		Set<Integer> widgetIds = getAppConfigure().getEntrySet();
		if (!widgetIds.isEmpty() || intent.getAction().equals(ACTION_ENABLE)) {
			profiles.put(Profile.A2dp, new ProfileEntry(new A2dpConnection()));
			profiles.put(Profile.Headset, new ProfileEntry(new HeadsetConnection(this, this)));
			mBroadcastReceiver.regist(this);
	
			updateBtState();
			for(int id : widgetIds) {
				updateWidget(id);
			}
		} else {
			stopSelf();
		}
		LOG.LEAVE();
	}

	@Override
	public void onDestroy() {
		LOG.ENTER();
		mBroadcastReceiver.unregist(this);
		for(ProfileEntry profile : profiles.values()) {
			profile.release();
		}
		profiles.clear();
		super.onDestroy();
	}

	@Override
	public IBinder onBind(Intent intent) {
		LOG.ENTER(intent);
		LOG.LEAVE();
		return null;
	}

	public static void sendUpdateWidget(Context context, int[] ids) {
		Intent intent = new Intent(BtWidgetService.ACTION_UPDATE);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}

	public static void sendDeleteWidget(Context context, int[] ids) {
		Intent intent = new Intent(BtWidgetService.ACTION_DELETE);
		intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
		context.sendBroadcast(intent);
	}

	@Override
	public void onServiceConnected() {
		updateBtState();
	}

	@Override
	public void onServiceDisconnected() {
	}

	private AppBroadcastReceiver.Action action(
			String name,
			ActionCommand command) {
		return new AppBroadcastReceiver.Action(name, command); 
	}

	private final AppBroadcastReceiver mBroadcastReceiver = new AppBroadcastReceiver(
			action(ACTION_UPDATE, new ActionCommand() {
				@Override public void action(Intent intent) {
					for(int id : intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
						updateWidget(id);
					}
				}
			}),
			action(ACTION_DELETE,  new ActionCommand() {
				@Override public void action(Intent intent) {
					for(int id : intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)) {
						deleteWidget(id);
					}
				}
			}),
			action(ACTION_SELECT,  new ActionCommand() {
				@Override public void action(Intent intent) {
					selectWidget(intent.getIntExtra(
							AppWidgetManager.EXTRA_APPWIDGET_ID,
							AppWidgetManager.INVALID_APPWIDGET_ID));
				}
			}),
			action(BluetoothAdapter.ACTION_STATE_CHANGED,  new ActionCommand() {
				@Override public void action(Intent intent) {
					updateBtState();
					if (requestId != AppWidgetManager.INVALID_APPWIDGET_ID) {
						if (btState == BtState.On) {
							selectWidget(requestId);
						}
					}
				}
			}),
			action(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED,  new ActionCommand() {
				@Override public void action(Intent intent) {
					updateBtState();
				}
			}),
			action(BluetoothDevice.ACTION_ACL_CONNECTED,  new ActionCommand() {
				@Override public void action(Intent intent) {
					updateBtState();
				}
			}),
			action(BluetoothDevice.ACTION_ACL_DISCONNECTED,  new ActionCommand() {
				@Override public void action(Intent intent) {
					updateBtState();
				}
			}),
			action("android.bluetooth.headset.action.STATE_CHANGED", new ActionCommand() {
				@Override public void action(Intent intent) {
					updateBtState();
					Widget widget = getWidget(requestId);
					if (widget != null && widget.configure.profile == Profile.Headset) {
						ProfileEntry profile = profiles.get(widget.configure.profile);
						if (profile.currentDevices.isEmpty() && btState == BtState.On) {
							selectWidget(requestId);
							requestId = AppWidgetManager.INVALID_APPWIDGET_ID;
						}
					}
				}
			}),
			action("android.bluetooth.a2dp.action.SINK_STATE_CHANGED", new ActionCommand() {
				@Override public void action(Intent intent) {
					updateBtState();
					Widget widget = getWidget(requestId);
					if (widget != null && widget.configure.profile == Profile.A2dp) {
						ProfileEntry profile = profiles.get(widget.configure.profile);
						if (profile.currentDevices.isEmpty() && btState == BtState.On) {
							selectWidget(requestId);
							requestId = AppWidgetManager.INVALID_APPWIDGET_ID;
						}
					}
				}
			}));

	private void selectWidget(int id) {
		LOG.ENTER("id:"+id);
		requestId = AppWidgetManager.INVALID_APPWIDGET_ID;
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		Widget widget = getWidget(id);
		ProfileEntry profile = profiles.get(widget.configure.profile);
		if (widget != null) {
			BluetoothDevice device = adapter.getRemoteDevice(widget.configure.address);
			boolean connected = profile.currentDevices.contains(device.getAddress());		
			if (connected) {
				profile.disconnect(device);
			} else {
				if (btState == BtState.Off) {
					LOG.DEBUG("  state:off");
					requestId = id;
					adapter.enable();
				} else if (btState == BtState.TurnningOn) {
					LOG.DEBUG("  state:turnning-on");
					requestId = id;
				} else if (btState == BtState.On) {
					LOG.DEBUG("  state:on");
					if (profile.currentDevices.isEmpty()) {
						LOG.DEBUG("  connection:false");
						profile.connect(device);
					} else {
						LOG.DEBUG("  connection:true");
						requestId = id;
						profile.disconnect();
					}
				}
			}
		} else {
			LOG.ERROR("The specified widget is not found. id:%d", id);
		}
	}

	private void updateWidget(int widgetId) {
		LOG.ENTER("id:"+widgetId);
		Widget widget = getWidget(widgetId);
		if (widget != null) {
			widget.update();
		} else {
			try {
				widget = new Widget(widgetId);
				widgets.add(widget);
				widget.update();
			} catch (NoPreferenceFoundError e) {
				LOG.ERROR("The specified widget dosen't have a configuration. id:%d", widgetId);
				LOG.ERROR("  exception:%s", e.getMessage());
			}
		}
	}

	private void deleteWidget(int widgetId) {
		LOG.ENTER("id:"+widgetId);
		Widget widget = getWidget(widgetId);
		if (widget != null) {
			widget.remove();
		}
	}

	private void updateBtState() {
		LOG.ENTER();
		// update bluetooth state
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if (adapter != null) {
			btState = BtState.fromeCode(adapter.getState());
		} else {
			btState = BtState.Off;
		}
		LOG.DEBUG("  state:%s", btState.code);
		// update each profile connections
		for(ProfileEntry profile : profiles.values()) {
			profile.updateCurrentDevices();
		}
		// update widget view
		for(Widget i : widgets) {
			i.update();
		}
		LOG.LEAVE();
	}

	private enum BtState {
		Off(BluetoothAdapter.STATE_OFF),
		On(BluetoothAdapter.STATE_ON),
		TurnningOn(BluetoothAdapter.STATE_TURNING_OFF),
		TurnningOff(BluetoothAdapter.STATE_TURNING_ON);

		private BtState(int code) {
			this.code = code;
		}

		static BtState fromeCode(int code) {
			for(BtState i : BtState.values()) {
				if (i.code == code) return i;
			}
			return BtState.Off;
		}

		private final int code;
	}

	private Widget getWidget(int id) {
		for(Widget i : widgets) {
			if (i.id == id) return i;
		}
		return null;
	}

	private class Widget {
		final int id;
		final WidgetConfigure configure;
		final PendingIntent pendingIntent;

		Widget(int id) throws NoPreferenceFoundError {
			LOG.ENTER();
			this.id = id;
			this.configure = getAppConfigure().getConfigure(id);
			Intent selectIntent = new Intent(ACTION_SELECT);
			selectIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, this.id);
			pendingIntent = PendingIntent.getBroadcast(
					BtWidgetService.this,
					this.id,
					selectIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			LOG.DEBUG("  id:%d", this.id);
			LOG.DEBUG("  name:%s", this.configure.name);
			LOG.DEBUG("  address:%s", this.configure.address);
			LOG.DEBUG("  profile:%s", this.configure.profile.code);
		}

		void update() {
			LOG.ENTER();
			LOG.DEBUG("  id:%d", this.id);
			LOG.DEBUG("  name:%s", this.configure.name);
			LOG.DEBUG("  address:%s", this.configure.address);
			LOG.DEBUG("  profile:%s", this.configure.profile.code);
			ProfileEntry profile = profiles.get(configure.profile);
			RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget);
			views.setOnClickPendingIntent(R.id.icon, pendingIntent);
			views.setTextViewText(R.id.text, configure.name);
			views.setTextViewText(R.id.profile, configure.profile.code);
			if (profile.currentDevices.contains(this.configure.address)) {
				views.setImageViewResource(R.id.icon, this.configure.profile.onIconResId);
			} else {
				views.setImageViewResource(R.id.icon, this.configure.profile.offIconResId);
			}
			AppWidgetManager manager = AppWidgetManager.getInstance(BtWidgetService.this);
			manager.updateAppWidget(this.id, views);
		}

		void remove() {
			LOG.ENTER();
			getAppConfigure().unregitConfigure(this.configure);
		}
	}

}
