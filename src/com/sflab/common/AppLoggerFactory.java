package com.sflab.common;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class AppLoggerFactory {

	private final Map<String, Level> mEnableList;

	private static enum Level {
		Debug,
		Information,
		Warrning,
		Error;

	   private static final String STR_DEBUG = "debug";
	   private static final String STR_INFO = "info";
	   private static final String STR_WARRN = "warrn";
	   private static final String STR_ERROR = "error";
	
		public static Level fromString(String str) {
			if (STR_DEBUG.equals(str)) {
				return Debug;
			} else if (STR_INFO.equals(str)) {
				return Information;
			} else if (STR_WARRN.equals(str)) {
				return Warrning;
			} else if (STR_ERROR.equals(str)) {
				return Error;
			} else {
				throw new IllegalStateException();
			}
		}

		public String toString() {
			switch(this) {
			case Debug: return STR_DEBUG;
			case Information: return STR_INFO;
			case Warrning: return STR_WARRN;
			case Error: return STR_ERROR;
			default: return "";
			}
		}
	}

	public AppLoggerFactory(String path) {
		mEnableList = new HashMap<String, Level>();
		try {
			config(path);
		} catch(IllegalStateException e) {
		}
	}

	public <T> AppLogger get(Class<T> clazz) {
		Level level = mEnableList.get(clazz);
		if (level != null) {
			return new DefaultLogger(clazz.getSimpleName());
		} else {
			return new DefaultLogger(clazz.getSimpleName());
		}
	}

	private void config(String configfile) {
		File file = new File(configfile);
		mEnableList.clear();
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(file);
			NodeList list = document.getChildNodes();
			for(int i=0; i<list.getLength(); i++) {
				Node node = list.item(i);
				if ("item".equals(node.getNodeName())) {
					NamedNodeMap attributes = node.getAttributes();
					Node name = attributes.getNamedItem("name");
					Node level = attributes.getNamedItem("level");
					if (name != null && level != null) {
						mEnableList.put(name.toString(), Level.fromString(level.toString()));
					}
				}
			}
		} catch(Exception e) {
			//throw new RuntimeException(e);
		}
	}

	private static class DefaultLogger implements AppLogger {
		private String mTag;

		public DefaultLogger(String tag) {
			mTag = tag;
		}

		public void DEBUG(String format, Object...args) {
			Log.d(mTag, String.format(format, args));
		}

		public void INFO(String format, Object...args) {
			Log.i(mTag, String.format(format, args));
		}

		public void WARRING(String format, Object...args) {
			Log.w(mTag, String.format(format, args));
		}

		public void ERROR(String format, Object...args) {
			Log.e(mTag, String.format(format, args));
		}

		public void ENTER(Object...args) {
			final StringBuilder builder = new StringBuilder(131);
			if(args.length > 0) {
				builder.append(args[0]);
				for(int i=1; i < args.length; i++) {
					builder.append(args[i]);
				}
			}
			DEBUG(String.format("ENTR%s(%s)", currentMethodInfo(4), builder.toString()));
		}

		public void LEAVE(Object...args) {
			final StringBuilder builder = new StringBuilder(131);
			if(args.length > 0) {
				builder.append(args[0]);
				for(int i=1; i < args.length; i++) {
					builder.append(args[i]);
				}
			}
			DEBUG(String.format("EXIT%s(%s)", currentMethodInfo(4), builder.toString()));
		}

		private String currentMethodInfo(int depth) {
			final StackTraceElement trace[] = Thread.currentThread().getStackTrace();
			if (trace.length > depth) {
				final String methodName = trace[depth].getMethodName();
				final String className = trace[depth].getClassName();
				final int head = className.lastIndexOf(".") + 1;
				return String.format(":%-3d %s#%s",
						trace.length,
						(head < 0 ? className : className.substring(head)),
						methodName);
			} else {
				return "N/A";
			}
		}
	}

	private static class DummyLogger implements AppLogger {

		public DummyLogger() {
		}

		public void DEBUG(String format, Object...args) {
		}

		public void INFO(String format, Object...args) {
		}

		public void WARRING(String format, Object...args) {
		}

		public void ERROR(String format, Object...args) {
		}

		public void ENTER(Object...args) {
		}

		public void LEAVE(Object...args) {
		}

	}
}
