package com.sflab.bluetooth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.sflab.bluetooth.AppConfigure.HasAppConfigure;
import com.sflab.bluetooth.AppConfigure.WidgetConfigure;
import com.sflab.bluetooth.Constants.Profile;
import com.sflab.common.AppLogger;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.view.View;
import android.view.Window;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class BtWidgetConfigure extends Activity implements HasAppConfigure,
		OnItemClickListener {

	private static final AppLogger LOG = Constants.LOGGER
			.get(BtWidgetConfigure.class);

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private ViewFlipper flipperView;
	private ListView profileList;
	private ListView deviceList;

	private Profile profile;
	private Item item;

	public AppConfigure getAppConfigure() {
		return (AppConfigure) getApplicationContext();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		LOG.ENTER();
		super.onCreate(savedInstanceState);

		// get any data we were launched with
		Intent launchIntent = getIntent();
		Bundle extras = launchIntent.getExtras();
		if (extras != null) {
			appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID,
					AppWidgetManager.INVALID_APPWIDGET_ID);
			Intent cancelResultValue = new Intent();
			cancelResultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
					appWidgetId);
			setResult(RESULT_CANCELED, cancelResultValue);
		} else {
			// only launch if it's for configuration
			// Note: when you launch for debugging, this does prevent this
			// activity from running. We could also turn off the intent
			// filtering for main activity.
			// But, to debug this activity, we can also just comment the
			// following line out.
			finish();
		}

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.configure);

		flipperView = (ViewFlipper) findViewById(R.id.flipper);
		profileList = (ListView) findViewById(R.id.profile_list);
		deviceList = (ListView) findViewById(R.id.device_list);

		initProfileList();
	}

	@Override
	protected void onDestroy() {
		LOG.ENTER();
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		LOG.ENTER();
		super.onPause();
	}

	@Override
	protected void onResume() {
		LOG.ENTER();
		super.onResume();
	}

	private ParcelUuid[] getUuids(BluetoothDevice device) {
		try {
			Method methodGetUuids = BluetoothDevice.class.getMethod("getUuids");
			return (ParcelUuid[]) methodGetUuids.invoke(device);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return new ParcelUuid[0];
	}

	private void initProfileList() {
		ArrayAdapter<Profile> adapter = new ArrayAdapter<Profile>(this,
				android.R.layout.simple_list_item_1, Profile.values());
		profileList.setAdapter(adapter);
		profileList.setOnItemClickListener(this);
	}

	private void initDeviceList() {
		LOG.ENTER();
		List<Item> items = new ArrayList<Item>();
		BluetoothAdapter bluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (bluetoothAdapter != null) {
			for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
				if (profile.isSupported(getUuids(device))) {
					items.add(Item
							.create(device.getName(), device.getAddress()));
				}
			}
		}
		ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(this,
				android.R.layout.simple_list_item_1, items);
		deviceList.setAdapter(adapter);
		deviceList.setOnItemClickListener(this);
	}

	@Override
	public void onBackPressed() {
		if (this.flipperView.getCurrentView() == this.flipperView.getChildAt(0)) {
			super.onBackPressed();
		} else {
			setupAnimation(true);
			this.flipperView.showPrevious();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> list, View view, int position,
			long id) {
		LOG.ENTER();
		if (list == profileList) {
			this.profile = (Profile) profileList.getAdapter().getItem(position);
			initDeviceList();
			setupAnimation(false);
			this.flipperView.showNext();
		} else if (list == deviceList) {
			this.item = (Item) deviceList.getAdapter().getItem(position);
			save();
			finish();
		}
	}

	private void save() {
		LOG.ENTER();
		getAppConfigure().registConfigure(
				new WidgetConfigure(appWidgetId, item.name, item.address,
						profile));
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		BtWidgetService.sendUpdateWidget(this, new int[] { this.appWidgetId });
	}

	private void setupAnimation(boolean back) {
		flipperView.setInAnimation(AnimationUtils.makeInAnimation(this, back));
		flipperView.setOutAnimation(AnimationUtils.makeOutAnimation(this, back));
	}

	private static class Item {
		Item(String name, String address) {
			this.name = name;
			this.address = address;
		}

		static Item create(String name, String address) {
			return new Item(name, address);
		}

		final String name;
		final String address;

		@Override
		public String toString() {
			return this.name;
		}
	}
}
