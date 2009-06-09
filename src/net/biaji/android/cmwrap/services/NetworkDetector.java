package net.biaji.android.cmwrap.services;

import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.Utils;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * 此类用于探查网络设置改变并决定隧道应用的开启与否以及手机启动后自动重启服务
 * 
 * @author biaji
 * 
 */
public class NetworkDetector extends BroadcastReceiver {

	private final String TAG = "CMWRAP->NetworkDetector";

	@Override
	public void onReceive(Context context, Intent intent) {
		Logger.v(TAG, "捕获事件：" + intent.getAction());

		Intent intentS = new Intent(context, WrapService.class);

		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			try {
				Thread.sleep(1000 * 60); // 等其它应用程序先抢一分钟
			} catch (InterruptedException e) {
				Logger.e(TAG, "谦逊失败");
			}
		}

		int level = Utils.getServiceLevel(context);

		// 在网络接入发生改变，而且当前链接非cmwap的情况下，暂停服务
		if (!Utils.isCmwap(context)) {
			intentS.putExtra("SERVERLEVEL", WrapService.SERVER_LEVEL_STOP);
			Logger.v(TAG, "目前不是cmwap接入，暂停服务");
		} else {
			if (level != WrapService.SERVER_LEVEL_NULL
					&& level != WrapService.SERVER_LEVEL_STOP) {
				intentS.putExtra("SERVERLEVEL", level);
			} else {
				intentS.putExtra("SERVERLEVEL", WrapService.SERVER_LEVEL_BASE);
			}
		}

		context.startService(intentS);
	}

}
