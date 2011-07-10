package com.sflab.bluetooth.connection;

import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;

public interface BtConnection {

	void release();

	ParcelUuid getUuid();

	boolean connect(BluetoothDevice device);

	boolean disconnect(BluetoothDevice device);

	BluetoothDevice[] getConnectedDevices();
}
