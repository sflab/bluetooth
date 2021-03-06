package com.sflab.bluetooth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sflab.bluetooth.AppConfigure.HasAppConfigure;
import com.sflab.bluetooth.AppConfigure.WidgetConfigure;
import com.sflab.bluetooth.Constants.Profile;
import com.sflab.common.AppLogger;
import com.sflab.common.BindViewAdapter;

import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class BtWidgetConfigure extends Activity implements
		HasAppConfigure,
		OnClickListener,
		OnItemClickListener,
		OnCheckedChangeListener {

	private static final AppLogger LOG = Constants.LOGGER.get(BtWidgetConfigure.class);

	private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

	private ViewFlipper flipperView;
	private ListView profileList;
	private ListView deviceList;
	private TextView pairingMessage;
	private EditText textEditName;
	private EditText textDefaultName;
	private RadioButton checkDefaultName;
	private RadioButton checkEditName;
	private Button createButton;

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

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.configure);

		flipperView = (ViewFlipper) findViewById(R.id.flipper);
		profileList = (ListView) findViewById(R.id.profile_list);
		deviceList = (ListView) findViewById(R.id.device_list);
		pairingMessage = (TextView) findViewById(R.id.pairing_message);
		pairingMessage.setOnClickListener(this);
		textEditName = (EditText) findViewById(R.id.text_edit_name);
		textDefaultName = (EditText) findViewById(R.id.text_default_name);
		checkDefaultName = (RadioButton) findViewById(R.id.radio_default_name);
		checkDefaultName.setOnCheckedChangeListener(this);
		checkEditName = (RadioButton) findViewById(R.id.radio_edit_name);
		checkEditName.setOnCheckedChangeListener(this);
		createButton = (Button) findViewById(R.id.button_create);
		createButton.setOnClickListener(this);
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
		initDeviceList();
	}

	private void initProfileList() {
		BindViewAdapter.ViewBinder<Profile> viewBinder = new BindViewAdapter.ViewBinder<Profile>() {
			@Override
			public View bind(View view, Profile item) {
				View text = view.findViewById(android.R.id.text1);
				if (text instanceof TextView) {
					((TextView)text).setText(item.toString());
				}
				return view;
			}
		};
		BindViewAdapter<Profile> adapter = new BindViewAdapter<Profile>(
				this,
				R.layout.list_item_with_arrow,
				viewBinder);
		adapter.setSource(Arrays.asList(Profile.values()));
		profileList.setAdapter(adapter);
		profileList.setOnItemClickListener(this);
	}

	private void initDeviceList() {
		LOG.ENTER();
		List<Item> items = new ArrayList<Item>();
		if (profile != null) {
			BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			if (bluetoothAdapter != null) {
				for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
					if (profile.isSupported(device)) {
						items.add(Item.create(device.getName(), device.getAddress()));
					}
				}
			}
		}

		if (Constants.USE_TESTDATA) {
			items.add(Item.create("test-device", "00000000"));
		}

		BindViewAdapter.ViewBinder<Item> viewBinder = new BindViewAdapter.ViewBinder<Item>() {
			@Override
			public View bind(View view, Item item) {
				View text = view.findViewById(android.R.id.text1);
				if (text instanceof TextView) {
					((TextView)text).setText(item.name);
				}
				return view;
			}
		};
		BindViewAdapter<Item> adapter = new BindViewAdapter<Item>(
				this,
				R.layout.list_item_with_arrow,
				viewBinder);
		adapter.setSource(items);

		if (adapter.getCount() > 0) {
			pairingMessage.setVisibility(View.INVISIBLE);
		} else {
			pairingMessage.setVisibility(View.VISIBLE);
		}

		deviceList.setAdapter(adapter);
		deviceList.setOnItemClickListener(this);
	}

	private void initEditName() {
		textDefaultName.setText(item.name);
		updateRadioButton();
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
	public void onClick(View v) {
		if (v == pairingMessage) {
			launchBluetoothSettings();

		} else if (v == createButton) {
			save();
			finish();
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
			initEditName();
			setupAnimation(false);
			this.flipperView.showNext();
		}
	}


	@Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
		updateRadioButton();
	}

	private void updateRadioButton() {
		textDefaultName.setEnabled(checkDefaultName.isChecked());
		textEditName.setEnabled(checkEditName.isChecked());
	}

	private void save() {
		LOG.ENTER();
		if (checkDefaultName.isChecked()) {
			getAppConfigure().registConfigure(new WidgetConfigure(
					appWidgetId,
					item.name,
					item.address,
					profile));

		} else {
			getAppConfigure().registConfigure(new WidgetConfigure(
					appWidgetId,
					textEditName.getText().toString(),
					item.address,
					profile));
		}
		Intent resultValue = new Intent();
		resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
		setResult(RESULT_OK, resultValue);
		BtWidgetService.sendUpdateWidget(this, new int[] { this.appWidgetId });
	}

	private void setupAnimation(boolean back) {
		flipperView.setInAnimation(AnimationUtils.makeInAnimation(this, back));
		flipperView.setOutAnimation(AnimationUtils.makeOutAnimation(this, back));
	}

	private void launchBluetoothSettings() {
		try {
			Intent intentBluetooth = new Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
			startActivity(intentBluetooth);
		} catch(Exception e) {
		}
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
