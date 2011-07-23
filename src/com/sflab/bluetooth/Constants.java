package com.sflab.bluetooth;

import android.bluetooth.BluetoothUuid;
import android.os.Environment;
import android.os.ParcelUuid;

import com.sflab.common.AppLoggerFactory;

public class Constants {
	public static final String ACTION_CREATE_WIDGET = "ACTION_CREATE_WIDGET";
	public static final String ACTION_BLUETOOTH_CONNECT = "ACTION_BLUETOOTH_CONNECT";
	public static final String ACTION_BLUETOOTH_DISCONNECT = "ACTION_BLUETOOTH_DISCONNECT";
	public static final String EXTRA_BLUETOOTH_ADDRESS = "EXTRA_BLUETOOTH_ADDRESS";

	public static final AppLoggerFactory LOGGER = new AppLoggerFactory(
			"Bluetooth", AppLoggerFactory.Level.Error,
			AppLoggerFactory.Level.Information, Environment
					.getExternalStorageDirectory().getPath()
					+ "/log.config");

	public static class NoProfileFoundError extends Exception {
		private static final long serialVersionUID = -1865411696886457810L;
	}

	public enum Profile {
		A2dp("A2DP", new ParcelUuid[] { BluetoothUuid.AdvAudioDist,
				BluetoothUuid.AudioSink }, R.string.a2dp_label,
				R.drawable.bluetooth_on_a2dp, R.drawable.bluetooth_off), Headset(
				"HSP", new ParcelUuid[] { BluetoothUuid.Handsfree,
						BluetoothUuid.HSP }, R.string.a2dp_label,
				R.drawable.bluetooth_on_hsp, R.drawable.bluetooth_off);

		Profile(String code, ParcelUuid[] uuids, int textResId,
				int oniconResId, int officonResId) {
			this.code = code;
			this.uuids = uuids;
			this.textResId = textResId;
			this.onIconResId = oniconResId;
			this.offIconResId = officonResId;
		}

		public boolean isSupported(ParcelUuid[] uuids) {
			for (ParcelUuid i : uuids) {
				for (ParcelUuid j : this.uuids) {
					if (i.equals(j))
						return true;
				}
			}
			return false;
		}

		@Override
		public String toString() {
			return this.code;
		}

		public static Profile fromCode(String code) throws NoProfileFoundError {
			for (Profile i : Profile.values()) {
				if (i.code.equals(code))
					return i;
			}
			throw new NoProfileFoundError();
		}

		public final int offIconResId;
		public final int onIconResId;
		public final int textResId;
		public final String code;
		private final ParcelUuid[] uuids;
	}
}
