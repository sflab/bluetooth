package com.sflab.bluetooth;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sflab.bluetooth.Constants.NoProfileFoundError;
import com.sflab.bluetooth.Constants.Profile;
import com.sflab.common.AppLogger;

import android.app.Application;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class AppConfigure extends Application implements OnSharedPreferenceChangeListener {
	private static final AppLogger LOG = Constants.LOGGER.get(AppConfigure.class);

    static final String PREFERENCES_NAME = "BluetoothWidgetPrefs";

    final Map<Integer, WidgetConfigure> cache;

    public interface HasAppConfigure {
    	AppConfigure getAppConfigure();
    }

    public static class NoPreferenceFoundError extends Exception {
		private static final long serialVersionUID = 2045852226392043348L;
    }

    public static class WidgetConfigure {
    	public final int id;
    	public final String name;
    	public final String address;
    	public final Profile profile;

    	public WidgetConfigure(int id, String name, String address, Profile profile) {
    		this.id = id;
    		this.name = name;
    		this.address = address;
    		this.profile = profile;
    	}

    	private void remove(SharedPreferences.Editor editor) {
    		editor.remove(Integer.toString(id));
    	}

    	private void pack(SharedPreferences.Editor editor) {
    		String value = String.format("%s,%s,%s", this.name, this.address, this.profile.code);
    		editor.putString(Integer.toString(id), value);
    	}

    	private static WidgetConfigure unpack(int id, SharedPreferences pref) throws NoPreferenceFoundError {
    		String value = pref.getString(Integer.toString(id), "");
    		String[] values = value.split(",");
    		if (values.length != 3) {
    			throw new NoPreferenceFoundError();
    		}
			String name = values[0];
			String address = values[1];
			String profile = values[2];
			LOG.DEBUG("unpack(id:%d, name:%s, address:%s, profile:%s)",
					id, name, address, profile);
			if (name.length() == 0 || address.length() == 0 || profile.length() == 0) {
				throw new NoPreferenceFoundError();
			}
			try {
				return new WidgetConfigure(id, name, address, Profile.fromCode(profile));
			} catch (NoProfileFoundError e) {
				throw new NoPreferenceFoundError();
			}
    	}

    	@Override
    	public String toString() {
    		return String.format("[name:%s,address:%s,profile:%s]",
    				this.name,
    				this.address,
    				this.profile.code);
    	}
    }

    public AppConfigure() {
    	cache = new HashMap<Integer, WidgetConfigure>();
    }

	@Override
	public void onCreate() {
		super.onCreate();
		SharedPreferences pref = getSharedPreferences(PREFERENCES_NAME, 0);
		pref.registerOnSharedPreferenceChangeListener(this);

		for(Entry<String, ?> entry : pref.getAll().entrySet()) {
			try {
				int id = Integer.parseInt(entry.getKey());
				WidgetConfigure config = WidgetConfigure.unpack(id, pref);
				cache.put(id, config);
			} catch(NumberFormatException e) {
				LOG.ERROR("can't parse the configuration for key:%s",entry.getKey());
			} catch (NoPreferenceFoundError e) {
				LOG.ERROR("can't parse the configuration for key:%s",entry.getKey());
			}
		}
	}

	@Override
	public void onTerminate() {
    	cache.clear();
		SharedPreferences pref = getSharedPreferences(PREFERENCES_NAME, 0);
		pref.unregisterOnSharedPreferenceChangeListener(this);
		super.onTerminate();
	}

	public boolean registConfigure(WidgetConfigure entry) {
		LOG.ENTER(entry);
		SharedPreferences config = getSharedPreferences(PREFERENCES_NAME, 0);
		SharedPreferences.Editor editor = config.edit();
		entry.pack(editor);
		return editor.commit();
	}

	public boolean unregitConfigure(WidgetConfigure entry) {
		LOG.ENTER(entry);
		SharedPreferences config = getSharedPreferences(PREFERENCES_NAME, 0);
		SharedPreferences.Editor editor = config.edit();
		entry.remove(editor);
		return editor.commit();
	}

	public WidgetConfigure getConfigure(int id) throws NoPreferenceFoundError {
		WidgetConfigure config = cache.get(id);
		if (config == null) {
			SharedPreferences pref = getSharedPreferences(PREFERENCES_NAME, 0);
			config = WidgetConfigure.unpack(id, pref);
			cache.put(config.id, config);
		}
		return config;
	}

	public Set<Integer> getEntrySet() {
		return cache.keySet();
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		try {
			int id = Integer.parseInt(key);
			cache.remove(id);
			WidgetConfigure config = WidgetConfigure.unpack(id, sharedPreferences);
			cache.put(id, config);
		} catch(NumberFormatException e) {
			LOG.ERROR("can't parse the configuration for key:%s",key);
		} catch (NoPreferenceFoundError e) {
			LOG.ERROR("can't parse the configuration for key:%s",key);
		}
	}
}
