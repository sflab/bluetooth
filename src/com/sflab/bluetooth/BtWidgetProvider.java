package com.sflab.bluetooth;

import com.sflab.common.AppLogger;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class BtWidgetProvider extends AppWidgetProvider {
	private static final AppLogger LOG = Constants.LOGGER
			.get(BtWidgetProvider.class);

	public static final Uri CONTENT_URI = Uri
			.parse("content://com.sflab.bluetooth");

	@Override
	public void onDeleted(Context context, int[] appWidgetIds) {
		LOG.ENTER();
		BtWidgetService.sendDeleteWidget(context, appWidgetIds);
		LOG.LEAVE();
	}

	@Override
	public void onDisabled(Context context) {
		LOG.ENTER();
		Intent intent = new Intent(context, BtWidgetService.class);
		context.stopService(intent);
		LOG.LEAVE();
	}

	@Override
	public void onEnabled(Context context) {
		LOG.ENTER();
		Intent intent = new Intent(context, BtWidgetService.class);
		intent.setAction(BtWidgetService.ACTION_ENABLE);
		context.startService(intent);
		LOG.LEAVE();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		LOG.ENTER(intent);
		if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
			Intent resumeIntent = new Intent(context, BtWidgetService.class);
			resumeIntent.setAction(BtWidgetService.ACTION_RESUME);
			context.startService(resumeIntent);
		} else {
			super.onReceive(context, intent);
		}
		LOG.LEAVE();
	}

	@Override
	public void onUpdate(Context context, AppWidgetManager appWidgetManager,
			int[] appWidgetIds) {
		LOG.ENTER();
		BtWidgetService.sendUpdateWidget(context, appWidgetIds);
		LOG.LEAVE();
	}
}
