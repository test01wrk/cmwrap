package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.util.ArrayList;

import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.Rule;
import net.biaji.android.cmwrap.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author biaji
 * 
 */
public class WrapService extends Service {

	private NotificationManager nm;

	private ArrayList<Rule> rules = new ArrayList<Rule>();

	private String proxyHost;

	private int proxyPort;

	private ArrayList<WrapServer> servers = new ArrayList<WrapServer>();

	private final String TAG = "CMWRAP->Service";

	private static boolean inService = false;

	@Override
	public void onCreate() {
		Log.v(TAG, "创建wrap服务");

		proxyHost = getResources().getString(R.string.proxyServer);
		proxyPort = Integer.parseInt(getResources().getString(
				R.string.proxyPort));

		// TODO 以下是一个丑陋的解决方案
		rules = Utils.loadRules(this);

		startSubDaemon();
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotify();
		inService = true;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {

		inService = false;

		for (WrapServer server : servers)
			if (!server.isClosed()) {
				try {
					server.close();
				} catch (IOException e) {
					Log.e(TAG, "Server " + server.getServPort() + "关闭错误", e);
				}
			}

		nm.cancel(R.string.serviceTagUp);
	}

	private void showNotify() {
		CharSequence notifyText = getText(R.string.serviceTagUp);
		Notification note = new Notification(R.drawable.notify, notifyText,
				System.currentTimeMillis());
		PendingIntent reviewIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, WrapService.class), 0);
		note.setLatestEventInfo(this, "cmwrap", notifyText, reviewIntent);
		nm.notify(R.string.serviceTagUp, note);
	}

	/**
	 * 启动侦听服务线程
	 */
	private void startSubDaemon() {

		for (Rule rule : rules) {
			if (rule.mode == Rule.MODE_SERV) {
				WrapServer server = new WrapServer(rule.name, rule.servPort,
						proxyHost, proxyPort);
				server.setDest(rule.desHost + ":" + rule.desPort);
				server.start();
				servers.add(server);
			}
		}
	}
}
