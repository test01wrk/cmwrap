package net.biaji.android.cmwrap.services;

import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * 此类用于探查网络设置改变并决定隧道应用的开启与否以及手机启动后自动重启服务
 * 
 * @author biaji
 * 
 */
public class NetworkDetector extends BroadcastReceiver {

	private final String TAG = "CMWRAP->NetworkDetector";

	/**
	 * 时间间隔，短于此间隔的变化不予实施
	 */
	private final long INTERVAL = 120;

	private static int inArray = 0;

	private String action = "";

	@Override
	public void onReceive(Context context, Intent intent) {

		action = intent.getAction();

		Logger.d(TAG, "捕获事件：" + action);

		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		boolean autoBoot = pref.getBoolean("AUTOBOOT", true);
		boolean autoChange = pref.getBoolean("AUTOCHANGE", true);

		if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {

			// 禁用自动启动
			if (!autoBoot) {
				Logger.d(TAG, "AUTOBOOT：" + autoBoot);
				return;
			}
			// 自动启动时，初始化iptables状态
			Utils.setIptableStatus(context, false);

		} else if (action.equals("android.net.conn.CONNECTIVITY_CHANGE")) {
			// 禁用自动状态切换
			if (!autoChange)
				return;
		}

		new initor(context).start();

	}

	private class initor extends Thread {

		Context context;

		initor(Context context) {
			this.context = context;
		}

		@Override
		public void run() {

			inArray++;

			Logger.d(TAG, inArray + "");
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(context);
			long latency = Long.parseLong(pref.getString("LATENCY", INTERVAL
					+ "")) * 1000;

			try {
				Thread.sleep(latency);
			} catch (InterruptedException e) {
				Logger.e(TAG, "休息，休息一会儿～～ 失败了");
			}

			if (inArray > 1) {
				inArray--;
				Logger.d(TAG, "撤销当前改变：" + action);
				return;
			}

			int level = Utils.getServiceLevel(context);
			Intent intentS = new Intent(context, WrapService.class);

			// 在网络接入发生改变，而且当前链接非cmwap的情况下，暂停服务
			if (!Utils.isCmwap(context)) {
				intentS.putExtra("SERVERLEVEL", WrapService.SERVER_LEVEL_STOP);
				Logger.v(TAG, "目前不是cmwap接入，暂停服务");
			} else {
				if (level != WrapService.SERVER_LEVEL_NULL
						&& level != WrapService.SERVER_LEVEL_STOP) {
					intentS.putExtra("SERVERLEVEL", level);
				} else {
					return;
				}
			}

			context.startService(intentS);

			inArray = 0;
		}
	}
}
