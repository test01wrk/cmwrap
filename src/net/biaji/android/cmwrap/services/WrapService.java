package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.R.drawable;
import net.biaji.android.cmwrap.R.string;

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

	private int servicePort = 5228;

	private ArrayList<WrapServer> servers;

	private final String TAG = "CMWRAP->Service";

	private boolean inService = false;
	

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Service#onCreate()
	 */
	@Override
	public void onCreate() {
		nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		WrapServer server = new WrapServer(servicePort);
		servers.add(server);
		server.start();
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
					Log.e(TAG, "Server " + server.getPort() + "关闭错误", e);
				}
			}

		nm.cancel(R.string.serviceTagUp);
		Toast.makeText(this, R.string.serviceTagDown, Toast.LENGTH_SHORT)
				.show();

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

}
