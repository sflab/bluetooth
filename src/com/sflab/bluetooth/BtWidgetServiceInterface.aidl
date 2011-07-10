package com.sflab.bluetooth;

interface BtWidgetServiceInterface {
	void connect(in int widgetId);
	void update(in int[] widgetIds);
	void delete(in int[] widgetIds);
}
