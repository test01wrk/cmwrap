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
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import android.widget.FrameLayout;

/**
 * @author biaji
 * 
 */
public class WrapService extends Service {

	private NotificationManager nm;

	private SharedPreferences pref;

	private ArrayList<Rule> rules = new ArrayList<Rule>();

	private String proxyHost;

	private int proxyPort;

	private ArrayList<WrapServer> servers = new ArrayList<WrapServer>();

	private final String TAG = "CMWRAP->Service";

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
	 * 此级别加入更多需要HTTP隧道的应用。
	 */
	public final static int SERVER_LEVEL_MORE_APPS = 3;

	private int serverLevel = SERVER_LEVEL_BASE;

	@Override
	public void onCreate() {
		Log.v(TAG, "创建wrap服务");

		// 由资源文件加载指定代理服务器
		proxyHost = getResources().getString(R.string.proxyServer);
		proxyPort = Integer.parseInt(getResources().getString(
				R.string.proxyPort));

		// 载入所有规则
		rules = Utils.loadRules(this);

		// 初始化通知管理器
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// 如果启动此服务时有原始级别，则使用之(可能是被系统蹂躏了)

		pref = getSharedPreferences("cmwrap", MODE_PRIVATE);

		int level = pref.getInt("SERVERLEVEL", SERVER_LEVEL_NULL);

		if (level != SERVER_LEVEL_NULL) {
			serverLevel = level;

			if (Utils.isCmwap(this)) {
				Utils.rootCMD(getString(R.string.CMDipForwardEnable));
				Utils.rootCMD(getString(R.string.CMDiptablesDisable));
				forward();
				startSubDaemon();
				showNotify();
			}
		}

	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);

		int level = intent.getIntExtra("SERVERLEVEL", SERVER_LEVEL_NULL);

		if (level != SERVER_LEVEL_NULL && level != serverLevel) {
			serverLevel = level;
			refreshSubDaemon();
		}

		// 在网络接入发生改变，而且当前链接非cmwap的情况下，暂停服务
		if (!Utils.isCmwap(this)) {
			Log.v(TAG, "目前不是cmwap接入，暂停服务");
			stopSubDaemon();
			Utils.rootCMD(getString(R.string.CMDiptablesDisable));
			serverLevel = SERVER_LEVEL_STOP;

		}

		showNotify();
		// 保存服务状态，以备被杀
		saveServiceLevel();

	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		stopSubDaemon();
		serverLevel = SERVER_LEVEL_NULL;
		saveServiceLevel();
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

		case SERVER_LEVEL_MORE_APPS:
			icon = R.drawable.notifysuper;
			notifyText = getText(R.string.serviceTagSuper);
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
			if (rule.mode < serverLevel) {
				if (rule.mode == Rule.MODE_SERV) {
					WrapServer server = new WrapServer(rule.name,
							rule.servPort, proxyHost, proxyPort);
					server.setDest(rule.desHost + ":" + rule.desPort);
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
					Log.e(TAG, "Server " + server.getServPort() + "关闭错误", e);
				}
			}
	}

	private void refreshSubDaemon() {
		stopSubDaemon();
		startSubDaemon();
	}

	private void forward() {

		try {
			for (Rule rule : rules) {
				String cmd;
				if (rule.mode == Rule.MODE_BASE)
					cmd = "iptables -t nat -A OUTPUT -o rmnet0 -p tcp --dport "
							+ rule.desPort + " -j DNAT --to-destination "
							+ proxyHost + ":" + proxyPort;
				else
					cmd = "iptables -t nat -A OUTPUT -o rmnet0 -p tcp -d "
							+ rule.desHost + " --dport " + rule.desPort
							+ " -j DNAT --to-destination 127.0.0.1:"
							+ rule.servPort;
				Utils.rootCMD(cmd);

			}

		} catch (Exception e) {
		}

	}

	/**
	 * 记录当前服务状态
	 */
	private void saveServiceLevel() {
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt("SERVERLEVEL", serverLevel);
		editor.commit();
	}
}
