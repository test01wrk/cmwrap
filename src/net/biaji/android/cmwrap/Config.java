/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *  
 *  此程序为开源软件，遵循GPLv3版本发布，并受其保护。
 *
 *  Copyright (c) 2009 by biAji
 */

package net.biaji.android.cmwrap;

import net.biaji.android.cmwrap.services.WrapService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.Preference.OnPreferenceChangeListener;

public class Config extends PreferenceActivity implements OnPreferenceChangeListener {

    private final static String TAG = "CMWRAP->Config";

    public static final String DEFAULT_DNS_ADD = "8.8.4.4";

    public static final String DEFAULT_HTTP_DNS_ADD = "http://dn5r3l4y.appspot.com";

    private EditTextPreference dnsadd;

    private CheckBoxPreference httpDns;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.config);
        dnsadd = (EditTextPreference) getPreferenceScreen().findPreference("DNSADD");
        httpDns = (CheckBoxPreference) getPreferenceScreen().findPreference("HTTPDNSENABLED");
    }

    @Override
    protected void onResume() {
        httpDns.setOnPreferenceChangeListener(this);
        super.onResume();
    }

    /**
     * 判断目前设置是否仅对cmwap进行代理处理
     * 
     * @param context
     * @return
     */
    public static boolean isCmwapOnly(Context context) {
        boolean result = true;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
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
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
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
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        result = pref.getBoolean("IPTABLES", false);
        Logger.v(TAG, "读取结束");
        return result;
    }

    /**
     * 记录当前服务状态
     */
    public static void saveServiceLevel(Context context, int level) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
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
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        result = pref.getInt("SERVERLEVEL", WrapService.SERVER_LEVEL_NULL);
        Logger.v(TAG, "读取结束");
        return result;
    }

    public static String getStringPref(Context context, String key, String defValue) {
        String result = "";
        Logger.v(TAG, "读取记录");
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        result = pref.getString(key, defValue);
        Logger.v(TAG, "读取结束");
        return result;
    }

    /**
     * 由配置文件读取布尔型配置
     * 
     * @param context
     * @param key 配置的名称
     * @param defValue 默认值
     * @return 指定配置的值
     */
    public static boolean getBooleanPref(Context context, String key, boolean defValue) {
        boolean result = false;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        result = pref.getBoolean(key, defValue);
        return result;
    }

    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference.getKey().equals("HTTPDNSENABLED")) {
            if (newValue.equals(true)) {
                dnsadd.setText(DEFAULT_HTTP_DNS_ADD);
                dnsadd.setSummary(getString(R.string.PREF_SUMMARY_DNS) + " " + DEFAULT_HTTP_DNS_ADD);
            } else {
                dnsadd.setText(DEFAULT_DNS_ADD);
                dnsadd.setSummary(getString(R.string.PREF_SUMMARY_DNS) + " " + DEFAULT_DNS_ADD);
            }
        }
        return true;
    }

}
