package net.biaji.android.cmwrap;

import java.io.DataInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import net.biaji.android.cmwrap.R.raw;
import android.content.ContextWrapper;
import android.util.Log;

public class Utils {

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
				Log.v("CMWRAP", "载入" + rule.name + "规则");
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

}
