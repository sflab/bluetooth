package com.sflab.common;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.util.Log;

public class AppLoggerFactory {
	private static final String TAG = AppLoggerFactory.class.getSimpleName();

	private final String mAppTag;
	private final Level mDefaultLevel;
	private final Level mTraceLevel;

	private final List<PackageWithLevel> mEntries;

	public AppLoggerFactory(String appTag, Level defaultLevel, Level traceLevel, String configFilename) {
		mAppTag = appTag;
		mDefaultLevel = defaultLevel;
		mTraceLevel = traceLevel;
		mEntries = new ArrayList<PackageWithLevel>();
		if (configFilename != null) {
			File configFile = new File(configFilename);
			try {
				config(configFile);
			} catch(IllegalStateException e) {
			}
		}
	}

	public AppLoggerFactory(String appTag, Level defaultLevel, Level traceLevel) {
		mAppTag = appTag;
		mDefaultLevel = defaultLevel;
		mTraceLevel = traceLevel;
		mEntries = new ArrayList<PackageWithLevel>();
	}

	public <T> AppLogger get(Class<T> clazz) {
		PackageName packageName = new PackageName(clazz.getName());
		Level level = null;
		for(PackageWithLevel e : mEntries) {
			if (e.mFilter.accept(packageName)) {
				level = e.mLevel;
				break;
			}
		}
		if (level != null) {
			return new DefaultLogger(level, clazz.getSimpleName());
		} else {
			return new DefaultLogger(mDefaultLevel, clazz.getSimpleName());
		}
	}

	private void config(File configFile) {
		Log.d(TAG, "config("+configFile.getPath()+")");
		mEntries.clear();
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		Node root = null;
		try {
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			Document document = builder.parse(configFile);
			NodeList list = document.getChildNodes();
			if (list.getLength() == 1) {
				root = list.item(0);
			}
		} catch(Exception e) {
		}
		if (root == null || !root.getNodeName().equals("configure")) {
			return;
		}

		NodeList nodes = root.getChildNodes();
		Log.d(TAG, "  count:" + nodes.getLength());
		for(int i=0; i<nodes.getLength(); i++) {
			Node node = nodes.item(i);
			Log.d(TAG, "  name:" + node.getNodeName());
			if ("item".equals(node.getNodeName())) {
				NamedNodeMap attributes = node.getAttributes();
				Node name = attributes.getNamedItem("name");
				Node level = attributes.getNamedItem("level");
				Log.d(TAG, "  > name:" + name.getNodeValue());
				Log.d(TAG, "  > level:" + level.getNodeValue());
				if (name != null && level != null) {
					try {
						mEntries.add(new PackageWithLevel(
								new PackageNameFilter(name.getNodeValue()),
								Level.fromString(level.getNodeValue())));
					} catch(IllegalArgumentException e) {
						Log.e(TAG, e.getMessage());
					}
				}
			}
		}

		Collections.sort(mEntries);

		Log.i(TAG, "Logger configure");
		for(PackageWithLevel e : mEntries) {
			Log.i(TAG, "  " + e);
		}
	}


	public static enum Level {
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
				throw new IllegalArgumentException();
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

	private static class PackageWithLevel implements Comparable<PackageWithLevel> {

		final PackageNameFilter mFilter;
		final Level mLevel;

		PackageWithLevel(PackageNameFilter filter, Level level) {
			mFilter = filter;
			mLevel = level;
		}

		@Override
		public int compareTo(PackageWithLevel another) {
			return mFilter.length() - another.mFilter.length();
		}

		@Override
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			for(String p : mFilter.mPackage.mParts) {
				if (p != mFilter.mPackage.mParts[0]) {
					buffer.append(".");
				}
				buffer.append(p);
			}
			buffer.append(": ");
			buffer.append(mLevel);
			return buffer.toString();
		}
	}

	private static class PackageName {
		private final String[] mParts;
		PackageName(String name) {
			StringTokenizer tokenizer = new StringTokenizer(name, ".");
			int i = 0;
			mParts = new String[tokenizer.countTokens()];
			while(tokenizer.hasMoreTokens() && i < mParts.length) {
				mParts[i++] = tokenizer.nextToken();
			}
		}
		int count() {
			return mParts.length;
		}
		String at(int index) {
			return mParts[index];
		}
	}

	private static class PackageNameFilter {
		private final PackageName mPackage;
		PackageNameFilter(String filter) {
			mPackage = new PackageName(filter);
		}
		public int length() {
			return mPackage.count();
		}
		public boolean accept(PackageName packageName) {
			if (mPackage.count() > packageName.count()) {
				return false;
			} else {
				for(int i=0; i<mPackage.count(); i++) {
					if (!mPackage.at(i).equals(packageName.at(i))) {
						return false;
					}
				}
			}
			return true;
		}
	}

	private class DefaultLogger implements AppLogger {
		private final String mTag;
		private final Level mLevel;

		public DefaultLogger(Level level, String tag) {
			mTag = tag;
			mLevel = level;
		}

		public void DEBUG(String format, Object...args) {
			write(Level.Debug, format, args);
		}

		public void INFO(String format, Object...args) {
			write(Level.Information, format, args);
		}

		public void WARRING(String format, Object...args) {
			write(Level.Warrning, format, args);
		}

		public void ERROR(String format, Object...args) {
			write(Level.Error, format, args);
		}

		public void ENTER(Object...args) {
			if (mTraceLevel.isHight(mLevel)) {
				final StringBuilder builder = new StringBuilder(131);
				if(args.length > 0) {
					builder.append(args[0]);
					for(int i=1; i < args.length; i++) {
						builder.append(args[i]);
					}
				}
				write(mTraceLevel, String.format("ENTR%s(%s)", currentMethodInfo(4), builder.toString()));
			}
		}

		public void LEAVE(Object...args) {
			if (mTraceLevel.isHight(mLevel)) {
				final StringBuilder builder = new StringBuilder(131);
				if(args.length > 0) {
					builder.append(args[0]);
					for(int i=1; i < args.length; i++) {
						builder.append(args[i]);
					}
				}
				write(mTraceLevel, String.format("EXIT%s(%s)", currentMethodInfo(4), builder.toString()));
			}
		}

		private void write(Level level, String format, Object...args) {
			if (level.isHight(mLevel)) {
				switch(level) {
				case Debug:
					Log.d(mAppTag, "["+mTag+"] "+String.format(format, args));
					break;
				case Information:
					Log.i(mAppTag, "["+mTag+"] "+String.format(format, args));
					break;
				case Warrning:
					Log.w(mAppTag, "["+mTag+"] "+String.format(format, args));
					break;
				case Error:
					Log.e(mAppTag, "["+mTag+"] "+String.format(format, args));
					break;
				}
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
