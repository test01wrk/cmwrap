package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import net.biaji.android.cmwrap.R;
import net.biaji.android.cmwrap.WapChannel;
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

	private int servicePort = 7745;

	private Thread serviceThread;

	private ServerSocket server;

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
		try {
			server = new ServerSocket(servicePort);
			showNotify();
			Log.d(TAG, "Server Socket Start Sucesses");

			inService = true;

			serviceThread = new Thread() {
				@Override
				public void run() {
					while (inService) {
						try {
							// Log.d(TAG, "waiting for client");
							Socket socket = server.accept();
							// Log.d(TAG, "And Get One");
							WapChannel channel = new WapChannel(socket);
							channel.start();
						} catch (IOException e) {
							Log.e(TAG, "folk channelThread failed", e);
						}
					}
				}

			};
			serviceThread.start();
		} catch (IOException e) {
			Log.e(TAG, "build Server Socket Failed", e);
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {

		inService = false;

		if (!server.isClosed()) {
			try {
				server.close();
			} catch (IOException e) {
				Log.e(TAG, "", e);
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
