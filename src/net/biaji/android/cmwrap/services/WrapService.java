package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.util.ArrayList;

import net.biaji.android.cmwrap.Cmwrap;
import net.biaji.android.cmwrap.Config;
import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.Rule;
import net.biaji.android.cmwrap.Utils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.preference.PreferenceManager;

/**
 * @author biaji
 * 
 */
public class WrapService extends Service {

	private NotificationManager nm;

	private ArrayList<Rule> rules = new ArrayList<Rule>();

	private String DNSServer;

	private String proxyHost;

	private int proxyPort;

	private ArrayList<WrapServer> servers = new ArrayList<WrapServer>();

	private final String TAG = "CMWRAP->Service";

	private boolean inService = false;

	private boolean isUltraMode = false;

	// public final static int SERVER_LEVEL_NO_NETWORK = -100;

	/**
	 * 服务状态未设定
	 */
	public final static int SERVER_LEVEL_NULL = -1;

	/**
	 * 非cmwap接入时，停止服务
	 */
	public final static int SERVER_LEVEL_STOP = 0;

	/**
	 * 此级别仅保留iptables转向
	 */
	public final static int SERVER_LEVEL_BASE = 1;

	/**
	 * 此级别加入需要HTTP隧道的基本G1应用（Gmail，Gtalk，普通认证）。
	 */
	public final static int SERVER_LEVEL_APPS = 2;

	/**
	 * 此级别服务运行于前台服务模式
	 */
	public final static int SERVER_LEVEL_FROGROUND_SERVICE = 3;

	private int serverLevel = SERVER_LEVEL_BASE;

	private SharedPreferences pref;

	@Override
	public void onCreate() {
		Logger.d(TAG, "创建wrap服务");

		pref = PreferenceManager.getDefaultSharedPreferences(this);
		proxyHost = pref.getString("PROXYHOST", getResources().getString(
				R.string.proxyServer));
		proxyPort = Integer.parseInt(pref.getString("PROXYPORT", getResources()
				.getString(R.string.proxyPort)));
		isUltraMode = pref.getBoolean("ULTRAMODE", false);

		DNSServer = pref.getString("DNSADD", "4.3.2.1");

		// 载入所有规则
		rules = Utils.loadRules(this);

		// 初始化通知管理器
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 启用iptables转向
		forward();

		// 如果启动此服务时有原始级别，则使用之(可能是被系统蹂躏了)

		serverLevel = Config.getServiceLevel(this);

		if (serverLevel != SERVER_LEVEL_NULL) {

			if (Utils.isCmwap(this)) {
				if (isUltraMode) {
					serverLevel = SERVER_LEVEL_FROGROUND_SERVICE;
					setForeground(true);
				}
				startSubDaemon();
				inService = true;
				showNotify();
			} else {
				serverLevel = SERVER_LEVEL_STOP;
			}
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		int level = intent.getIntExtra("SERVERLEVEL", SERVER_LEVEL_NULL);
		Logger.d(TAG, "Level Change from " + serverLevel + " to Intent:"
				+ level);

		if (level != SERVER_LEVEL_NULL && level != serverLevel) {
			serverLevel = level;
			refreshSubDaemon();
		}

		showNotify();
		// 保存服务状态，以备被杀
		if (serverLevel != SERVER_LEVEL_STOP)
			Config.saveServiceLevel(this, serverLevel);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		stopSubDaemon();
		cleanForward();
		serverLevel = SERVER_LEVEL_NULL;
		Config.saveServiceLevel(this, serverLevel);
		nm.cancel(R.string.serviceTagUp);
	}

	private void showNotify() {

		CharSequence notifyText = getText(R.string.serviceTagUp);

		int icon = 0;
		switch (serverLevel) {
		case SERVER_LEVEL_STOP:
			icon = R.drawable.notifyinva;
			notifyText = getText(R.string.serviceTagDown);
			break;

		case SERVER_LEVEL_BASE:
			icon = R.drawable.notify;
			notifyText = getText(R.string.serviceTagUp);
			break;

		case SERVER_LEVEL_APPS:
			icon = R.drawable.notifybusy;
			notifyText = getText(R.string.serviceTagApp);
			break;

		case SERVER_LEVEL_FROGROUND_SERVICE:
			icon = R.drawable.notifysuper;
			notifyText = getText(R.string.serviceTagSuper);
			break;
		}

		Notification note = new Notification(icon, notifyText, System
				.currentTimeMillis());
		if (isUltraMode)
			note.flags = Notification.FLAG_ONGOING_EVENT;
		PendingIntent reviewIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Cmwrap.class), 0);
		note.setLatestEventInfo(this, getText(R.string.app_name), notifyText,
				reviewIntent);
		nm.notify(R.string.serviceTagUp, note);
	}

	/**
	 * 启动侦听服务线程
	 */
	private void startSubDaemon() {

		if (serverLevel <= SERVER_LEVEL_STOP)
			return;

		for (Rule rule : rules) {
			if (rule.mode < serverLevel) {
				if (rule.mode == Rule.MODE_SERV) {
					WrapServer server = ServerFactory.getServer(rule);
					server.setProxyHost(proxyHost);
					server.setProxyPort(proxyPort);
					server.start();
					servers.add(server);
				}
			}
		}
	}

	private void stopSubDaemon() {

		for (WrapServer server : servers)
			if (!server.isClosed()) {
				try {
					server.close();
				} catch (IOException e) {
					Logger.e(TAG, "Server " + server.getServPort() + "关闭错误", e);
				}
			}

	}

	private void refreshSubDaemon() {
		stopSubDaemon();
		startSubDaemon();
	}

	private void forward() {

		// 如果iptables处于已执行状态，则啥都不干
		if (Config.isIptablesEnabled(this))
			return;

		Utils.rootCMD(getString(R.string.CMDipForwardEnable));
		Utils.rootCMD(getString(R.string.CMDiptablesDisable));

		String inface = " ";
		boolean onlyCmwap = pref.getBoolean("ONLYCMWAP", true);

		if (onlyCmwap)
			inface = " -o rmnet0 ";

		for (Rule rule : rules) {
			//DNS规则跳过 
			if (rule.desHost != null && rule.desHost.equals("*"))
				continue;

			try {
				String protocol = " -p " + rule.protocol;

				String cmd = "iptables -t nat -A OUTPUT " + inface + protocol;

				if (rule.mode == Rule.MODE_BASE) 
					cmd += " --dport " + rule.desPort + " -j DNAT "
							+ " --to-destination " + proxyHost + ":"
							+ proxyPort;
				else {
					if (rule.desHost != null)
						cmd += " -d " + rule.desHost;

					cmd += " --dport " + rule.desPort
							+ " -j DNAT --to-destination 127.0.0.1:"
							+ rule.servPort;
				}
				Utils.rootCMD(cmd);

			} catch (Exception e) {
				Logger.e(TAG, e.getLocalizedMessage());
			}
		}

		Config.setIptableStatus(this, true);

	}

	private void cleanForward() {
		Utils.rootCMD(getString(R.string.CMDiptablesDisable));
	}

}
