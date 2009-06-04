package net.biaji.android.cmwrap.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

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
		Log.v(TAG, "捕获事件：" + intent.getAction());

		if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {
			try {
				Thread.sleep(1000 * 60); // 等其它应用程序先抢一分钟
			} catch (InterruptedException e) {
				Log.e(TAG, "谦逊失败");
			}
		}
		context.startService(new Intent(context, WrapService.class));
	}

}
