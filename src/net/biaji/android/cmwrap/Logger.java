
package net.biaji.android.cmwrap;

import android.util.Log;

public class Logger {

    private static int debugLevel = Log.VERBOSE;

    private static final String TAG = "CMWRAP";

    public static void v(String subTag, String msg) {
        if (debugLevel <= Log.VERBOSE)
            Log.v(TAG, subTag + "-> " + msg);
    }

    public static void d(String subTag, String msg) {
        if (debugLevel <= Log.DEBUG)
            Log.d(TAG, subTag + "-> " + msg);
    }

    public static void i(String subTag, String msg) {
        if (debugLevel <= Log.INFO)
            Log.i(TAG, subTag + "-> " + msg);
    }

    public static void w(String subTag, String msg) {
        if (debugLevel <= Log.VERBOSE)
            Log.w(TAG, subTag + "-> " + msg);
    }

    public static void e(String subTag, String msg) {
        if (debugLevel <= Log.ERROR)
            Log.e(TAG, subTag + "-> " + msg);
    }

    public static void e(String subTag, String msg, Throwable e) {
        if (debugLevel <= Log.ERROR)
            Log.e(TAG, subTag + "-> " + msg, e);
    }

}
