package net.biaji.android.cmwrap.services;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import net.biaji.android.cmwrap.Cmwrap;
import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.Rule;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

/**
 * @author biaji
 * 
 */
public class WrapService extends Service {

	private NotificationManager nm;

	private ArrayList<WrapServer> servers = new ArrayList<WrapServer>();

	private final String TAG = "CMWRAP->Service";

	private static boolean inService = false;

	@Override
	public void onCreate() {

		Log.v(TAG, "启用wrap服务");
		writeLog("创建wrap服务");
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		startSubDaemon();
		showNotify();

		inService = true;
	}

	@Override
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		writeLog("启动wrap服务");

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
		Toast.makeText(this, R.string.serviceTagDown, Toast.LENGTH_SHORT)
				.show();
		writeLog("销毁wrap服务");
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

		for (Rule rule : Cmwrap.getRules()) {

			if (rule.mode == Rule.MODE_SERV) {
				WrapServer server = new WrapServer(rule.servPort);
				server.setDest(rule.desHost + ":" + rule.desPort);
				server.start();
				servers.add(server);
				writeLog("启用" + rule.name + "服务于" + rule.servPort + "端口");
				Log.v(TAG, "启用" + rule.name + "服务于" + rule.servPort + "端口");
			}
		}
	}

	/**
	 * 在SD卡记录日志
	 * 
	 * @param log
	 */
	protected static void writeLog(String log) {
		FileWriter objFileWriter = null;

		try {
			Calendar objCalendar = Calendar.getInstance();
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
			String strDate = sdf.format(objCalendar.getTime());

			StringBuilder objStringBuilder = new StringBuilder();

			objStringBuilder.append(strDate);
			objStringBuilder.append(": ");
			objStringBuilder.append(log);
			objStringBuilder.append("\n");

			objFileWriter = new FileWriter("/sdcard/log.txt", true);
			objFileWriter.write(objStringBuilder.toString());
			objFileWriter.flush();
			objFileWriter.close();
		} catch (Exception e) {
			try {
				objFileWriter.close();
			} catch (Exception e2) {
			}
		}
	}
}
