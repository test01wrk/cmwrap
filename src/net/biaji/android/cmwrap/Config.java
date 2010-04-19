package net.biaji.android.cmwrap;

import net.biaji.android.cmwrap.services.WrapService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class Config extends PreferenceActivity {

	private final static String TAG = "CMWRAP->Config";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.config);
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
	 * 保存iptables运行状态
	 * 
	 * @param context
	 * @param iptables
	 */
	public static void setIptableStatus(Context context, boolean iptables) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("IPTABLES", iptables);
		editor.commit();
	}

	/**
	 * 获取iptables运行状态
	 * 
	 * @param context
	 * @return
	 */
	public static boolean isIptablesEnabled(Context context) {
		boolean result = false;
		Logger.v(TAG, "读取iptables");
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		result = pref.getBoolean("IPTABLES", false);
		Logger.v(TAG, "读取结束");
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

	public static String getStringPref(Context context, String key,
			String defValue) {
		String result = "";
		Logger.v(TAG, "读取记录");
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		result = pref.getString(key, defValue);
		Logger.v(TAG, "读取结束");
		return result;
	}

	/**
	 * 读取dns设置
	 * 
	 * @param context
	 * @return
	 */
	public static String getDNServer(Context context) {
		String dns = null;
		Logger.v(TAG, "读取DNS");
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		dns = pref.getString("DNSADD", "8.8.8.8");
		Logger.v(TAG, "读取结束");
		return dns;

	}

}
