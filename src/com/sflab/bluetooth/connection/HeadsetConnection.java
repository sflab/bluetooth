package com.sflab.bluetooth.connection;

import com.sflab.bluetooth.Constants;
import com.sflab.common.AppLogger;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothUuid;
import android.bluetooth.IBluetoothHeadset;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;

public class HeadsetConnection implements BtConnection {
	private static final AppLogger LOG = Constants.LOGGER.get(HeadsetConnection.class);

    /**
     * Create a BluetoothHeadset proxy object.
     */
    public HeadsetConnection(Context context, ServiceListener l) {
        mContext = context;
        mServiceListener = l;
        if (!context.bindService(new Intent(IBluetoothHeadset.class.getName()), mConnection, 0)) {
            LOG.ERROR("Could not bind to Bluetooth Headset Service");
        }
    }

	@Override
	public boolean connect(BluetoothDevice device) {
		return connectHeadset(device);
	}

	@Override
	public boolean disconnect(BluetoothDevice device) {
		return disconnectHeadset(device);
	}

	@Override
	public BluetoothDevice[] getConnectedDevices() {
		BluetoothDevice current = getCurrentHeadset();
		if (current == null) {
			return new BluetoothDevice[0];
		} else {
			return new BluetoothDevice[] {current};
		}
	}

	@Override
	public ParcelUuid getUuid() {
		return BluetoothUuid.Handsfree;
	}

	@Override
	public void release() {
		close();
	}

    public static final String ACTION_STATE_CHANGED =
            "android.bluetooth.headset.action.STATE_CHANGED";
    /**
     * TODO(API release): Consider incorporating as new state in
     * HEADSET_STATE_CHANGED
     */
    public static final String ACTION_AUDIO_STATE_CHANGED =
            "android.bluetooth.headset.action.AUDIO_STATE_CHANGED";
    public static final String EXTRA_STATE =
            "android.bluetooth.headset.extra.STATE";
    public static final String EXTRA_PREVIOUS_STATE =
            "android.bluetooth.headset.extra.PREVIOUS_STATE";
    public static final String EXTRA_AUDIO_STATE =
            "android.bluetooth.headset.extra.AUDIO_STATE";

    /** Extra to be used with the Headset State change intent.
     * This will be used only when Headset state changes to
     * {@link #STATE_DISCONNECTED} from any previous state.
     * This extra field is optional and will be used when
     * we have deterministic information regarding whether
     * the disconnect was initiated by the remote device or
     * by the local adapter.
     */
    public static final String EXTRA_DISCONNECT_INITIATOR =
            "android.bluetooth.headset.extra.DISCONNECT_INITIATOR";

    /**
     * TODO(API release): Consider incorporating as new state in
     * HEADSET_STATE_CHANGED
     */
    private IBluetoothHeadset mService;
    private final Context mContext;
    private final ServiceListener mServiceListener;

    /** There was an error trying to obtain the state */
    public static final int STATE_ERROR        = -1;
    /** No headset currently connected */
    public static final int STATE_DISCONNECTED = 0;
    /** Connection attempt in progress */
    public static final int STATE_CONNECTING   = 1;
    /** A headset is currently connected */
    public static final int STATE_CONNECTED    = 2;

    /** A SCO audio channel is not established */
    public static final int AUDIO_STATE_DISCONNECTED = 0;
    /** A SCO audio channel is established */
    public static final int AUDIO_STATE_CONNECTED = 1;

    public static final int RESULT_FAILURE = 0;
    public static final int RESULT_SUCCESS = 1;
    /** Connection canceled before completetion. */
    public static final int RESULT_CANCELED = 2;

    /** Values for {@link #EXTRA_DISCONNECT_INITIATOR} */
    public static final int REMOTE_DISCONNECT = 0;
    public static final int LOCAL_DISCONNECT = 1;


    /** Default priority for headsets that  for which we will accept
     * inconing connections and auto-connect */
    public static final int PRIORITY_AUTO_CONNECT = 1000;
    /** Default priority for headsets that  for which we will accept
     * inconing connections but not auto-connect */
    public static final int PRIORITY_ON = 100;
    /** Default priority for headsets that should not be auto-connected
     * and not allow incoming connections. */
    public static final int PRIORITY_OFF = 0;
    /** Default priority when not set or when the device is unpaired */
    public static final int PRIORITY_UNDEFINED = -1;

    /**
     * An interface for notifying BluetoothHeadset IPC clients when they have
     * been connected to the BluetoothHeadset service.
     */
    public interface ServiceListener {
        /**
         * Called to notify the client when this proxy object has been
         * connected to the BluetoothHeadset service. Clients must wait for
         * this callback before making IPC calls on the BluetoothHeadset
         * service.
         */
        public void onServiceConnected();

        /**
         * Called to notify the client that this proxy object has been
         * disconnected from the BluetoothHeadset service. Clients must not
         * make IPC calls on the BluetoothHeadset service after this callback.
         * This callback will currently only occur if the application hosting
         * the BluetoothHeadset service, but may be called more often in future.
         */
        public void onServiceDisconnected();
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    /**
     * Close the connection to the backing service.
     * Other public functions of BluetoothHeadset will return default error
     * results once close() has been called. Multiple invocations of close()
     * are ok.
     */
    public synchronized void close() {
        if (mConnection != null) {
            mContext.unbindService(mConnection);
            mConnection = null;
        }
    }

    /**
     * Get the current state of the Bluetooth Headset service.
     * @return One of the STATE_ return codes, or STATE_ERROR if this proxy
     *         object is currently not connected to the Headset service.
     */
    public int getState(BluetoothDevice device) {
        if (mService != null) {
            try {
                return mService.getState();
            } catch (RemoteException e) {
            }
        } else {
        }
        return STATE_ERROR;
    }

    /**
     * Get the BluetoothDevice for the current headset.
     * @return current headset, or null if not in connected or connecting
     *         state, or if this proxy object is not connected to the Headset
     *         service.
     */
    public BluetoothDevice getCurrentHeadset() {
        if (mService != null) {
            try {
                return mService.getCurrentHeadset();
            } catch (RemoteException e) {
            }
        } else {
        }
        return null;
    }

    /**
     * Request to initiate a connection to a headset.
     * This call does not block. Fails if a headset is already connecting
     * or connected.
     * Initiates auto-connection if device is null. Tries to connect to all
     * devices with priority greater than PRIORITY_AUTO in descending order.
     * @param device device to connect to, or null to auto-connect last connected
     *               headset
     * @return       false if there was a problem initiating the connection
     *               procedure, and no further HEADSET_STATE_CHANGED intents
     *               will be expected.
     */
    public boolean connectHeadset(BluetoothDevice device) {
        if (mService != null) {
            try {
                if (mService.connectHeadset(device)) {
                    return true;
                }
            } catch (RemoteException e) {
            	
            }
        } else {
        }
        return false;
    }

    /**
     * Returns true if the specified headset is connected (does not include
     * connecting). Returns false if not connected, or if this proxy object
     * if not currently connected to the headset service.
     */
    public boolean isConnected(BluetoothDevice device) {
        if (mService != null) {
            try {
                return mService.isConnected(device);
            } catch (RemoteException e) {
            }
        } else {
        }
        return false;
    }

    /**
     * Disconnects the current headset. Currently this call blocks, it may soon
     * be made asynchornous. Returns false if this proxy object is
     * not currently connected to the Headset service.
     */
    public boolean disconnectHeadset(BluetoothDevice device) {
        if (mService != null) {
            try {
                mService.disconnectHeadset();
                return true;
            } catch (RemoteException e) {}
        } else {
        }
        return false;
    }

	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mService = IBluetoothHeadset.Stub.asInterface(service);
			if (mServiceListener != null) {
				mServiceListener.onServiceConnected();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			mService = null;
			if (mServiceListener != null) {
				mServiceListener.onServiceDisconnected();
			}
		}
	};
}
