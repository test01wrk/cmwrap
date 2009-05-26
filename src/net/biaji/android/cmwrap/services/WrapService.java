package net.biaji.android.cmwrap.services;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;

import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.Rule;
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

		// TODO 以下是一个异常丑陋的解决方案
		loadRules();

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

	private void loadRules() {

		// if (inService)
		// return;

		if (rules.size() > 1)
			return;

		proxyHost = getResources().getString(R.string.proxyServer);
		proxyPort = Integer.parseInt(getResources().getString(
				R.string.proxyPort));

		DataInputStream in = null;
		try {
			in = new DataInputStream(getResources()
					.openRawResource(R.raw.rules));
			String line = "";
			while ((line = in.readLine()) != null) {

				Rule rule = new Rule();
				// if (line != null)
				// line = new String(line.trim().getBytes("UTF-8"));

				String[] items = line.split("\\|");

				rule.name = items[0];
				if (items.length > 2) {
					rule.mode = Rule.MODE_SERV;
					rule.desHost = items[1];
					rule.desPort = Integer.parseInt(items[2]);
					rule.servPort = Integer.parseInt(items[3]);
				} else if (items.length == 2) {
					rule.mode = Rule.MODE_BASE;
					rule.desPort = Integer.parseInt(items[1]);
				}
				Log.d(TAG, "载入" + rule.name + "规则");
				rules.add(rule);

			}
			in.close();
			in = null;
		} catch (Exception e) {
			Log.e(TAG, "载入规则文件失败：" + e.getLocalizedMessage());
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
				}
				in = null;
			}
		}

	}
}
