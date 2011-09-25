package com.sflab.bluetooth;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothUuid;
import android.os.ParcelUuid;

import com.sflab.common.AppLoggerFactory;

public class Constants {
	public static final String ACTION_CREATE_WIDGET = "ACTION_CREATE_WIDGET";
	public static final String ACTION_BLUETOOTH_CONNECT = "ACTION_BLUETOOTH_CONNECT";
	public static final String ACTION_BLUETOOTH_DISCONNECT = "ACTION_BLUETOOTH_DISCONNECT";
	public static final String EXTRA_BLUETOOTH_ADDRESS = "EXTRA_BLUETOOTH_ADDRESS";

	public static final AppLoggerFactory LOGGER = new AppLoggerFactory(
			"Bluetooth",
			AppLoggerFactory.Level.Warrning,
			AppLoggerFactory.Level.Information,
			null);

	public static final boolean USE_TESTDATA = false;

	public static class NoProfileFoundError extends Exception {
		private static final long serialVersionUID = -1865411696886457810L;
	}

	public enum Profile {
		A2dp(BluetoothProfile.A2DP,
				"A2DP",
				new ParcelUuid[] {
					BluetoothUuid.AdvAudioDist,
					BluetoothUuid.AudioSink },
				new int[] {
					BluetoothClass.Service.RENDER },
				new int[] {
					BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
					BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
					BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
					BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO },
				R.string.a2dp_label,
				R.drawable.bluetooth_a2dp,
				R.drawable.bluetooth_off),
		Headset(BluetoothProfile.HEADSET,
				"HSP",
				new ParcelUuid[] {
					BluetoothUuid.Handsfree,
					BluetoothUuid.HSP },
				new int[] { },
				new int[] {
					BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
					BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
					BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO },
				R.string.a2dp_label,
				R.drawable.bluetooth_hsp,
				R.drawable.bluetooth_off);

		Profile(
				int code,
				String label,
				ParcelUuid[] uuids,
				int[] serviceClasses,
				int[] deviceClasses,
				int textResId,
				int oniconResId,
				int officonResId) {
			this.code = code;
			this.label = label;
			this.uuids = uuids;
			this.serviceClasses = serviceClasses;
			this.deviceClasses = deviceClasses;
			this.textResId = textResId;
			this.onIconResId = oniconResId;
			this.offIconResId = officonResId;
		}

		public boolean isSupported(BluetoothDevice device) {
			ParcelUuid[] uuids = getUuids(device);
			if (uuids != null) {
				for (ParcelUuid i : uuids) {
					for (ParcelUuid j : this.uuids) {
						if (i.equals(j))
							return true;
					}
				}
			}
			BluetoothClass bluetoothClass = device.getBluetoothClass();
			for(int requireServiceClass : this.serviceClasses) {
				if (bluetoothClass.hasService(requireServiceClass)) {
					return true;
				}
			}
			for(int requireDeviceClass : this.deviceClasses) {
				if (requireDeviceClass == bluetoothClass.getDeviceClass()) {
					return true;
				}
			}
			return false;
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

		@Override
		public String toString() {
			return this.label;
		}

		public static Profile fromLabel(String label) throws NoProfileFoundError {
			for (Profile i : Profile.values()) {
				if (i.label.equals(label))
					return i;
			}
			throw new NoProfileFoundError();
		}

		public final int code;
		public final String label;
		public final int offIconResId;
		public final int onIconResId;
		public final int textResId;
		private final ParcelUuid[] uuids;
		private final int[] serviceClasses;
		private final int[] deviceClasses;
	}
}
