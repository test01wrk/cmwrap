package net.biaji.android.cmwrap.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import net.biaji.android.cmwrap.Config;
import net.biaji.android.cmwrap.Logger;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class Utils {

	private final static String TAG = "CMWRAP->Utils";

	public static String errMsg = "";

	public static String getErr() {
		return errMsg;
	}

	public static String bytesToHexString(byte[] bytes) {
		return bytesToHexString(bytes, 0, bytes.length);
	}

	public static String bytesToHexString(byte[] bytes, int start, int offset) {
		String result = "";

		String stmp = "";
		for (int n = start; n < offset; n++) {
			stmp = (Integer.toHexString(bytes[n] & 0XFF));
			if (stmp.length() == 1)
				result = result + "0" + stmp;
			else
				result = result + stmp;

		}
		return result.toUpperCase();
	}

	/**
	 * 判断当前网络连接是否为cmwap
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isCmwap(Context context) {

		// 根据配置情况决定是否检查当前数据连接
		if (!Config.isCmwapOnly(context))
			return true;

		// -------------------

		ConnectivityManager manager = (ConnectivityManager) context
				.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = manager.getActiveNetworkInfo();
		if (networkInfo == null
				|| networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
			return false;

		boolean result = false;

		Cursor mCursor = context.getContentResolver().query(
				Uri.parse("content://telephony/carriers"),
				new String[] { "apn" }, "current=1", null, null);
		if (mCursor != null) {
			try {
				if (mCursor.moveToFirst()) {
					String name = mCursor.getString(0);
					if (name != null && name.trim().equalsIgnoreCase("cmwap"))
						result = true;
				}
			} catch (Exception e) {
				Logger.e(TAG, "Can not get Network info", e);
			} finally {
				mCursor.close();
			}
		}
		return result;
	}

	/**
	 * 重新设置DNS服务器地址
	 * 
	 * @param dns
	 *            服务器地址 默认8.8.8.8
	 * @param context
	 */
	public static void flushDns(String dns, Context context) {
		if (dns == null || dns.equals(""))
			dns = "8.8.8.8";
		String setcmd;

		// if (isCmwap(context)) --不符合国情
		// setcmd = "setprop net.rmnet0.dns1 ";
		// else
		setcmd = "setprop net.dns1 ";
		rootCMD(setcmd + dns);
	}

	/**
	 * 以root权限执行命令
	 * 
	 * @param 需要执行的指令
	 * @return -1 执行失败； 0 执行正常
	 */
	public static synchronized int rootCMD(String cmd) {
		int result = -1;
		DataOutputStream os = null;
		InputStream err = null, out = null;
		try {
			Process process = Runtime.getRuntime().exec("su");
			err = process.getErrorStream();
			BufferedReader bre = new BufferedReader(new InputStreamReader(err),
					1024 * 8);

			out = process.getInputStream();

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(cmd + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();

			String resp;
			while ((resp = bre.readLine()) != null) {
				Logger.d(TAG, resp);
				errMsg = resp;
			}
			result = process.waitFor();
			if (result == 0)
				Logger.d(TAG, cmd + " exec success");
			else {
				Logger.d(TAG, cmd + " exec with result " + result);
			}
			os.close();
			process.destroy();
		} catch (IOException e) {
			Logger.e(TAG, "Failed to exec command", e);
		} catch (InterruptedException e) {
			Logger.e(TAG, "线程意外终止", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
			}

		}

		return result;
	}

	public static byte[] int2byte(int res) {
		byte[] targets = new byte[4];

		targets[0] = (byte) (res & 0xff);// 最低位
		targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
		targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
		targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
		return targets;
	}
}
