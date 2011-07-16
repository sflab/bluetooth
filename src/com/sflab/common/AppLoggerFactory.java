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

	private static final boolean ENABLE_CONFIG = true;
	private static final Level DEFAULT_LEVEL = Level.Error;
	private static final Level TRACE_LEVEL = Level.Information;

	private final Map<String, Level> mEnableList;

	private static enum Level {
		Debug(0),
		Information(1),
		Warrning(2),
		Error(3);

		private static final String STR_DEBUG = "debug";
		private static final String STR_INFO = "info";
		private static final String STR_WARRN = "warrn";
		private static final String STR_ERROR = "error";

		private final int index;

		Level(int index) {
			this.index = index;
		}

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

		public boolean isHight(Level other) {
			return this.index >= other.index;
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
		if (ENABLE_CONFIG) {
			try {
				config(path);
			} catch(IllegalStateException e) {
			}
		}
	}

	public <T> AppLogger get(Class<T> clazz) {
		Level level = mEnableList.get(clazz);
		if (level != null) {
			return new DefaultLogger(level, clazz.getSimpleName());
		} else {
			return new DefaultLogger(DEFAULT_LEVEL, clazz.getSimpleName());
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
		private final String mTag;
		private final Level mLevel;

		public DefaultLogger(Level level, String tag) {
			mTag = tag;
			mLevel = level;
		}

		public void DEBUG(String format, Object...args) {
			if (Level.Debug.isHight(mLevel)) {
				Log.d(mTag, String.format(format, args));
			}
		}

		public void INFO(String format, Object...args) {
			if (Level.Information.isHight(mLevel)) {
				Log.i(mTag, String.format(format, args));
			}
		}

		public void WARRING(String format, Object...args) {
			if (Level.Warrning.isHight(mLevel)) {
				Log.w(mTag, String.format(format, args));
			}
		}

		public void ERROR(String format, Object...args) {
			if (Level.Error.isHight(mLevel)) {
				Log.e(mTag, String.format(format, args));
			}
		}

		public void ENTER(Object...args) {
			if (TRACE_LEVEL.isHight(mLevel)) {
				final StringBuilder builder = new StringBuilder(131);
				if(args.length > 0) {
					builder.append(args[0]);
					for(int i=1; i < args.length; i++) {
						builder.append(args[i]);
					}
				}
				DEBUG(String.format("ENTR%s(%s)", currentMethodInfo(4), builder.toString()));
			}
		}

		public void LEAVE(Object...args) {
			if (TRACE_LEVEL.isHight(mLevel)) {
				final StringBuilder builder = new StringBuilder(131);
				if(args.length > 0) {
					builder.append(args[0]);
					for(int i=1; i < args.length; i++) {
						builder.append(args[i]);
					}
				}
				DEBUG(String.format("EXIT%s(%s)", currentMethodInfo(4), builder.toString()));
			}
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
}
