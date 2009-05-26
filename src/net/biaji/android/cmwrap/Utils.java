package net.biaji.android.cmwrap;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

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

}
