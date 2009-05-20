package net.biaji.android.cmwrap;

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

}
