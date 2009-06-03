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

	/**
	 * 非cmwap接入时，停止服务
	 */
	public final static int SERVER_LEVEL_STOP = 0;

	/**
	 * 此级别仅保留iptables转向
	 */
	public final static int SERVER_LEVEL_BASE = 1;

	/**
	 * 此级别加入需要HTTP隧道的应用。
	 */
	public final static int SERVER_LEVEL_APPS = 2;

	private static int serverLevel = SERVER_LEVEL_BASE;

	@Override
	public void onCreate() {
		Log.v(TAG, "创建wrap服务");

		proxyHost = getResources().getString(R.string.proxyServer);
		proxyPort = Integer.parseInt(getResources().getString(
				R.string.proxyPort));

		// TODO 以下是一个丑陋的解决方案
		rules = Utils.loadRules(this);

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		startSubDaemon();
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		showNotify();
		inService = true;

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

		int icon = 0;
		switch (serverLevel) {
		case SERVER_LEVEL_STOP:
			icon = R.drawable.notifyinva;
			break;

		case SERVER_LEVEL_BASE:
			icon = R.drawable.notify;
			break;

		case SERVER_LEVEL_APPS:
			icon = R.drawable.notifybusy;
			break;
		}

		Notification note = new Notification(icon, notifyText, System
				.currentTimeMillis());
		PendingIntent reviewIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, WrapService.class), 0);
		note.setLatestEventInfo(this, getText(R.string.app_name), notifyText,
				reviewIntent);
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
