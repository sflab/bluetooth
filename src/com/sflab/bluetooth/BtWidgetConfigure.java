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
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;

public class BtWidgetConfigure extends Activity
implements HasAppConfigure, OnClickListener, OnItemClickListener {

	private static final AppLogger LOG = Constants.LOGGER.get(BtWidgetConfigure.class);

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private ViewFlipper flipperView;
	private View profileSelectView;
	private View deviceSelectView;
	private View nameEditView;

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
            appWidgetId = extras.getInt(
            		AppWidgetManager.EXTRA_APPWIDGET_ID,
            		AppWidgetManager.INVALID_APPWIDGET_ID);
            Intent cancelResultValue = new Intent();
            cancelResultValue.putExtra(
            		AppWidgetManager.EXTRA_APPWIDGET_ID,
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

        setContentView(R.layout.configure);

        flipperView = (ViewFlipper) findViewById(R.id.flipper);
    	profileSelectView = (View) findViewById(R.id.profile_select);
    	deviceSelectView = (View) findViewById(R.id.device_select);
    	nameEditView = (View) findViewById(R.id.name_edit);
    	
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
        ArrayAdapter<Profile> adapter = new ArrayAdapter<Profile>(
        		this,
        		android.R.layout.simple_list_item_1,
        		Profile.values());
        ListView list = (ListView) profileSelectView.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        TextView title = (TextView) profileSelectView.findViewById(R.id.title);
        title.setText("Select Bluetooth Profile");
	}

	private void initDeviceList() {
        List<Item> items = new ArrayList<Item>();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null) {
	        for(BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
	        	if (profile.isSupported(getUuids(device))) {
	    	        items.add(Item.create(device.getName(), device.getAddress()));
	        	}
	        }
        }
        ArrayAdapter<Item> adapter = new ArrayAdapter<Item>(
        		this,
        		android.R.layout.simple_list_item_1,
        		items);
        ListView list = (ListView) deviceSelectView.findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(this);
        TextView title = (TextView) deviceSelectView.findViewById(R.id.title);
        title.setText("Select Bluetooth Device");
	}

	private void initWidgetName() {
		TextView text = (TextView) nameEditView.findViewById(R.id.text);
		text.setText(item.name);
		Button button = (Button) nameEditView.findViewById(R.id.button);
		button.setOnClickListener(this);
        TextView title = (TextView) nameEditView.findViewById(R.id.title);
        title.setText("Edit Widget Name");
	}

	@Override
	public void onBackPressed() {
		if (this.flipperView.getCurrentView() == this.flipperView.getChildAt(0)) {
			super.onBackPressed();
		} else {
			this.flipperView.showPrevious();
		}
	}

	@Override
	public void onItemClick(AdapterView<?> list, View view, int position, long id) {
		LOG.ENTER();
        ListView profileList = (ListView) profileSelectView.findViewById(R.id.list);
        ListView deviceList = (ListView) deviceSelectView.findViewById(R.id.list);
        if (list == profileList) {
        	this.profile = (Profile) profileList.getAdapter().getItem(position);
        	initDeviceList();
        } else if (list == deviceList) {
    		this.item = (Item) deviceList.getAdapter().getItem(position);
    		initWidgetName();
        }
        this.flipperView.showNext();
	}

	@Override
	public void onClick(View v) {
		TextView text = (TextView) nameEditView.findViewById(R.id.text);
		getAppConfigure().registConfigure(new WidgetConfigure(
				appWidgetId,
				text.getText().toString(),
				item.address,
				profile));
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		BtWidgetService.sendUpdateWidget(this, new int[] { this.appWidgetId });
		finish();
	}

	static class Item {
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
