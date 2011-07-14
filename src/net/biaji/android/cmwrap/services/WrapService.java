package net.biaji.android.cmwrap.services;

import net.biaji.android.cmwrap.Cmwrap;
import net.biaji.android.cmwrap.Config;
import net.biaji.android.cmwrap.IptablesManager;
import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.utils.Utils;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.IBinder;
import android.preference.PreferenceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 穿越服务总控制 <br>
 * 
 * @author biaji<biaji@biaji.net>
 */
public class WrapService extends Service {

	private NotificationManager nm;

	private String DNSServer;

	private String proxyHost;

	private int proxyPort;

	private IptablesManager iptablesManager = IptablesManager.getInstance();

	private ArrayList<WrapServer> servers = new ArrayList<WrapServer>();

	private final String TAG = "Service";

	private final int ONE_AND_ONLY_NOTIFY = 0;

	private boolean isUltraMode = false, dnsEnabled = true,
			dnsHttpEnabled = false, httpOnly = false;

	/**
	 * 服务级别未设定
	 */
	public final static int SERVER_LEVEL_NULL = -2;

	/**
	 * 禁用服务
	 */
	public final static int SERVER_LEVEL_STOP = -1;

	/**
	 * 非cmwap接入时，暂停服务
	 */
	public final static int SERVER_LEVEL_PAUSE = 0;

	/**
	 * 此级别仅保留iptables转向
	 */
	public final static int SERVER_LEVEL_BASE = 1;

	/**
	 * 此级别加入需要HTTP隧道的应用（Gmail，Gtalk，普通认证）。
	 */
	public final static int SERVER_LEVEL_APPS = 2;

	/**
	 * 此级别服务运行于前台服务模式
	 */
	public final static int SERVER_LEVEL_FROGROUND_SERVICE = 3;

	/**
	 * 服务级别的确认原则如下：
	 * <ol>
	 * <li>如有intent传入，则使用intent指定的级别</li>
	 * <li>如有上次记录的服务级别，则服务为被强制关闭后重启，使用记录的服务级别</li>
	 * <li>否则依据配置确认服务级别</li>
	 * <ol>
	 * 其取值为{@link SERVER_LEVEL_NULL},{@link SERVER_LEVEL_STOP},
	 * {@link SERVER_LEVEL_BASE},{@link SERVER_LEVEL_APPS},
	 * {@link SERVER_LEVEL_FROGROUND_SERVICE}
	 */
	private int serverLevel = SERVER_LEVEL_STOP;

	private SharedPreferences pref;

	@Override
	public void onCreate() {
		Logger.d(TAG, "创建wrap服务");

		pref = PreferenceManager.getDefaultSharedPreferences(this);
		proxyHost = pref
				.getString("PROXYHOST", getString(R.string.proxyServer));
		proxyPort = Integer.parseInt(pref.getString("PROXYPORT",
				getString(R.string.proxyPort)));
		isUltraMode = pref.getBoolean("ULTRAMODE", false);
		dnsEnabled = pref.getBoolean("DNSENABLED", true);
		dnsHttpEnabled = pref.getBoolean("HTTPDNSENABLED", false);
		httpOnly = pref.getBoolean("ONLYHTTP", false);
		DNSServer = pref.getString("DNSADD", Config.DEFAULT_DNS_ADD);

		iptablesManager.setProxyHost(proxyHost);
		iptablesManager.setProxyPort(proxyPort);

		// 初始化通知管理器
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 读取初始服务级别
		serverLevel = Config.getServiceLevel(this);
		Logger.d(TAG, "Recovery from server level: " + serverLevel);

		// 如果无初始服务级别，则为启动过程，依据配置确定启动级别
		if (serverLevel == SERVER_LEVEL_STOP) {
			if (isUltraMode) {
				serverLevel = SERVER_LEVEL_FROGROUND_SERVICE;
				if (VERSION.SDK_INT < VERSION_CODES.ECLAIR)
					setForeground(true);
			} else if (httpOnly) {
				serverLevel = SERVER_LEVEL_BASE;
			} else {
				serverLevel = SERVER_LEVEL_APPS;
			}
		}

		// 如果启动此服务时有原始级别，则使用之(可能是被系统蹂躏了)
		if (Utils.isCmwap(this)) {
			startSubDaemon();
			showNotify();
		} else {
			serverLevel = SERVER_LEVEL_PAUSE;
			cleanForward();
		}
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		int level = SERVER_LEVEL_NULL;

		if (intent != null)
			level = intent.getIntExtra("SERVERLEVEL", SERVER_LEVEL_NULL);

		if (level != SERVER_LEVEL_NULL)
			changeLevelTo(level);

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		stopSubDaemon();
		serverLevel = SERVER_LEVEL_STOP;
		Config.saveServiceLevel(this, serverLevel);
		nm.cancel(ONE_AND_ONLY_NOTIFY);
	}

	/**
	 * 将服务级别设置为新的级别
	 * 
	 * @param newLevel
	 *            欲设置的服务级别
	 */
	private void changeLevelTo(int newLevel) {

		Logger.d(TAG, "Level Change from " + serverLevel + " to:" + newLevel);

		if (this.serverLevel != newLevel) {
			serverLevel = newLevel;
			refreshSubDaemon();
			showNotify();
		}

	}

	/**
	 * 显示提示
	 */
	private void showNotify() {

		CharSequence notifyText = getText(R.string.serviceTagUp);

		int icon = 0;
		switch (serverLevel) {
		case SERVER_LEVEL_PAUSE:
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

		Notification notify = new Notification(icon, notifyText, System
				.currentTimeMillis());

		PendingIntent reviewIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, Cmwrap.class), 0);
		notify.setLatestEventInfo(this, getText(R.string.app_name), notifyText,
				reviewIntent);
		if (isUltraMode) { // 处理前台服务
			notify.flags = Notification.FLAG_ONGOING_EVENT;
			if (VERSION.SDK_INT >= VERSION_CODES.ECLAIR)
				startForeground(0, notify);
		}
		nm.notify(ONE_AND_ONLY_NOTIFY, notify);
	}

	/**
	 * 启动子服务线程
	 */
	private void startSubDaemon() {

		if (serverLevel < SERVER_LEVEL_BASE)
			return;

		if (dnsEnabled) {

			DNSServer dnsSer;

			if (dnsHttpEnabled)
				dnsSer = new DNSServerHttp("DNS HTTP Proxy", 7442, proxyHost,
						proxyPort, DNSServer, 80);
			else
				dnsSer = new DNSServer("DNS Proxy", 7442, proxyHost, proxyPort,
						DNSServer, 53);

			Logger.d(TAG, "Start DNS server");

			dnsSer.setBasePath(this.getFilesDir().getParent());
			new Thread(dnsSer).start();
			servers.add(dnsSer);
			iptablesManager.addAllRules(Arrays.asList(dnsSer.getRules()));
		}

		NormalTcpServer tcpSer = new NormalTcpServer("Tcp Tunnel", proxyHost,
				proxyPort);
		new Thread(tcpSer).start();
		servers.add(tcpSer);

		iptablesManager.addAllRules(Arrays.asList(tcpSer.getRules()));

		forward();
		Config.saveServiceLevel(this, serverLevel);

	}

	private void stopSubDaemon() {

		cleanForward();

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

	/**
	 * 根据当前设置，启用iptables
	 */
	private void forward() {

		// 如果iptables处于已执行状态，则啥都不干
		if (Config.isIptablesEnabled(this))
			return;
		iptablesManager.enable();
		Config.setIptableStatus(this, true);
	}

	private void cleanForward() {
		iptablesManager.disable();
		Config.setIptableStatus(this, false);
	}

}
