package net.biaji.android.cmwrap;

import android.util.Log;

//TODO 由String加载日志级别
public class Logger {

	private static int debugLevel = Log.WARN;

	public static void v(String TAG, String msg) {
		if (debugLevel >= Log.VERBOSE)
			Log.d(TAG, msg);
	}

	public static void d(String TAG, String msg) {
		if (debugLevel >= Log.DEBUG)
			Log.d(TAG, msg);
	}

	public static void i(String TAG, String msg) {
		if (debugLevel >= Log.INFO)
			Log.i(TAG, msg);
	}

	public static void e(String TAG, String msg) {
		if (debugLevel >= Log.ERROR)
			Log.e(TAG, msg);
	}

	public static void e(String TAG, String msg, Throwable e) {
		if (debugLevel >= Log.ERROR)
			Log.e(TAG, msg, e);
	}

}
