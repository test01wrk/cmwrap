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

}
