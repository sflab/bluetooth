package com.sflab.bluetooth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothProfile.ServiceListener;
import android.content.Context;

public class BtConnection implements ServiceListener {

	private final int profile;
	private final BluetoothAdapter adapter;
	private final ServiceListener listener;
	private final Method funcConnect;
	private final Method funcDisconnect;

	private BluetoothProfile proxy;

	private boolean isClosed;

	public BtConnection(
			Context context,
			ServiceListener listener,
			int profile) {

		this.adapter = BluetoothAdapter.getDefaultAdapter();
		this.proxy = null;
		this.listener = listener;
		this.profile = profile;
		this.isClosed = false;

		this.adapter.getProfileProxy(
				context,
				this,
				this.profile);

		Method funcConnect;
		Method funcDisconnect;
		try {
			funcConnect = BluetoothProfile.class.getMethod(
					"connect", BluetoothDevice.class);
			funcDisconnect = BluetoothProfile.class.getMethod(
					"disconnect", BluetoothDevice.class);

		} catch (SecurityException e) {
			funcConnect = null;
			funcDisconnect = null;
		} catch (NoSuchMethodException e) {
			funcConnect = null;
			funcDisconnect = null;
		}

		this.funcConnect = funcConnect;
		this.funcDisconnect = funcDisconnect;
	}

	public boolean isClosed() {
		return isClosed;
	}

	public boolean isConnected() {
		return (proxy != null);
	}

	public void close() {
		isClosed = true;
		if (proxy != null) {
			this.adapter.closeProfileProxy(profile, proxy);
			proxy = null;
		}
	}

	public boolean connect(BluetoothDevice device) {
		if (funcConnect != null && proxy != null) {
			try {
				return (Boolean)funcConnect.invoke(proxy, device);

			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		return false;
	}

	public boolean disconnect(BluetoothDevice device) {
		if (funcDisconnect != null && proxy != null) {
			try {
				return (Boolean)funcDisconnect.invoke(proxy, device);

			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (InvocationTargetException e) {
			}
		}
		return false;
	}

	public List<BluetoothDevice> getConnectedDevices() {
		if (proxy != null) {
			return proxy.getConnectedDevices();
		} else {
			return Collections.emptyList();
		}
	}

	public int getConnectionState(BluetoothDevice device) {
		if (proxy != null) {
			return proxy.getConnectionState(device);
		} else {
			return BluetoothProfile.STATE_DISCONNECTED;
		}
	}

	@Override
	public void onServiceConnected(int profile, BluetoothProfile proxy) {
		if (this.profile == profile) {
			if (isClosed) {
				close();
			} else {
				this.proxy = proxy;
				this.listener.onServiceConnected(profile, proxy);
			}
		}
	}

	@Override
	public void onServiceDisconnected(int profile) {
		if (this.profile == profile) {
			if (isClosed) {
			} else {
				this.proxy = null;
				this.listener.onServiceDisconnected(profile);
			}
		}
	}
}
