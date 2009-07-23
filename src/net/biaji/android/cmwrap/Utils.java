package net.biaji.android.cmwrap;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import net.biaji.android.cmwrap.services.WrapService;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.preference.PreferenceManager;

public class Utils {

	private static String TAG = "CMWRAP->Utils";

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
	 * 在SD卡记录日志
	 * 
	 * @param log
	 */
	public static void writeLog(String log) {
		FileWriter objFileWriter = null;

		try {
			Calendar objCalendar = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
			String strDate = sdf.format(objCalendar.getTime());

			StringBuilder objStringBuilder = new StringBuilder();

			objStringBuilder.append(strDate);
			objStringBuilder.append(": ");
			objStringBuilder.append(log);
			objStringBuilder.append("\n");

			objFileWriter = new FileWriter("/sdcard/log.txt", true);
			objFileWriter.write(objStringBuilder.toString());
			objFileWriter.flush();
			objFileWriter.close();
		} catch (Exception e) {
			try {
				objFileWriter.close();
			} catch (Exception e2) {
			}
		}
	}

	/**
	 * 载入转向规则
	 */
	public static ArrayList<Rule> loadRules(ContextWrapper context) {

		ArrayList<Rule> rules = new ArrayList<Rule>();

		DataInputStream in = null;
		try {
			in = new DataInputStream(context.getResources().openRawResource(
					R.raw.rules));
			String line = "";
			while ((line = in.readLine()) != null) {

				Rule rule = new Rule();
				// if (line != null)
				// line = new String(line.trim().getBytes("UTF-8"));

				String[] items = line.split("\\|");

				rule.name = items[0];
				if (items.length > 2) {
					rule.mode = Rule.MODE_SERV;
					rule.desHost = items[1];
					rule.desPort = Integer.parseInt(items[2]);
					rule.servPort = Integer.parseInt(items[3]);
				} else if (items.length == 2) {
					rule.mode = Rule.MODE_BASE;
					rule.desPort = Integer.parseInt(items[1]);
				}
				Logger.v(TAG, "载入" + rule.name + "规则");
				rules.add(rule);

			}
			in.close();
			in = null;
		} catch (Exception e) {
			Logger.e("CMWRAP", "载入规则文件失败：" + e.getLocalizedMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
				in = null;
			}
		}
		return rules;
	}

	/**
	 * 判断当前网络连接是否为cmwap
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isCmwap(Context context) {

		// 根据配置情况决定是否检查当前数据连接
		if (!isCmwapOnly(context))
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
				Logger.e(TAG, "Can not get Network info");
			} finally {
				mCursor.close();
			}
		}
		return result;
	}

	/**
	 * 判断目前设置是否仅对cmwap进行代理处理
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isCmwapOnly(Context context) {
		boolean result = true;
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		result = pref.getBoolean("ONLYCMWAP", true);
		return result;
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
				Logger.d(TAG, cmd + " exec with result" + result);
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

	/**
	 * 记录当前服务状态
	 */
	public static void saveServiceLevel(Context context, int level) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt("SERVERLEVEL", level);
		editor.commit();
	}

	/**
	 * 获取当前服务状态
	 */
	public static int getServiceLevel(Context context) {
		int result = WrapService.SERVER_LEVEL_NULL;
		Logger.v(TAG, "读取记录");
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		result = pref.getInt("SERVERLEVEL", WrapService.SERVER_LEVEL_NULL);
		Logger.v(TAG, "读取结束");
		return result;
	}

	/**
	 * 获取iptables运行状态
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isIptable(Context context) {
		boolean result = false;
		Logger.v(TAG, "读取iptables");
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		result = pref.getBoolean("IPTABLES", false);
		Logger.v(TAG, "读取结束");
		return result;
	}

	/**
	 * 保存iptables运行状态
	 * 
	 * @param context
	 * @param iptables
	 */
	public static void putIptable(Context context, boolean iptables) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("IPTABLES", iptables);
		editor.commit();
	}
}
