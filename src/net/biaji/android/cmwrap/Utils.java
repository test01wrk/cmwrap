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

import net.biaji.android.cmwrap.R.raw;
import android.content.ContextWrapper;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class Utils {

	private static String TAG = "CMWRAP->Utils";

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
				Log.v(TAG, "载入" + rule.name + "规则");
				rules.add(rule);

			}
			in.close();
			in = null;
		} catch (Exception e) {
			Log.e("CMWRAP", "载入规则文件失败：" + e.getLocalizedMessage());
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
	 * TODO  把wifi去掉
	 * @param context
	 * @return
	 */
	public static boolean isCmwap(ContextWrapper context) {
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
				Log.e(TAG, "Can not get Network info");
			} finally {
				mCursor.close();
			}
		}
		return result;
	}

	/**
	 * 以root权限执行命令
	 * 
	 * @param 需要执行的指令
	 * @return -1 执行失败； 0 执行正常
	 */
	public static int rootCMD(String cmd) {
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
				Log.d(TAG, resp);
			}
			result = process.waitFor();
			if (result == 0)
				Log.d(TAG, cmd + " exec success");
			else {
				Log.d(TAG, cmd + " exec with result" + result);
			}
			os.close();
			process.destroy();
		} catch (IOException e) {
			Log.e(TAG, "Failed to exec command", e);
		} catch (InterruptedException e) {
			Log.e(TAG, "线程意外终止", e);
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

}
