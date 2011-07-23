package com.sflab.bluetooth.connection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothA2dp;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;

public class A2dpConnection implements BtConnection {

	private static final String SERVICE_NAME = "bluetooth_a2dp";
	private IBluetoothA2dp mService = null;

	public A2dpConnection() {

		try {
			Class<?> classServiceManager = Class
					.forName("android.os.ServiceManager");
			Method methodGetService = classServiceManager.getMethod(
					"getService", String.class);
			IBinder binder = (IBinder) methodGetService.invoke(null,
					SERVICE_NAME);
			mService = IBluetoothA2dp.Stub.asInterface(binder);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public void release() {
		mService = null;
	}

	public ParcelUuid getUuid() {
		return BluetoothUuid.AdvAudioDist;
	}

	public boolean connect(BluetoothDevice device) {
		if (mService == null || device == null) {
			return false;
		}
		try {
			mService.connectSink(device);
		} catch (RemoteException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	public boolean disconnect(BluetoothDevice device) {
		if (mService == null || device == null) {
			return false;
		}
		try {
			mService.disconnectSink(device);
		} catch (RemoteException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	@Override
	public BluetoothDevice[] getConnectedDevices() {
		try {
			return mService.getConnectedSinks();
		} catch (RemoteException e) {
			e.printStackTrace();
			return new BluetoothDevice[0];
		}
	}
}
