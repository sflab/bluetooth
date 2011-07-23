package com.sflab.common;

public interface AppLogger {

	void DEBUG(String format, Object...args);

	void INFO(String format, Object...args);

	void WARRING(String format, Object...args);

	void ERROR(String format, Object...args);

	void ENTER(Object...args);

	void LEAVE(Object...args);

}
