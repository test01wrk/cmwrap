package net.biaji.android.cmwrap.services;

import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import net.biaji.android.cmwrap.Utils;

import android.R;
import android.util.Log;

public class WapChannel extends Thread {

	private long starTime = System.currentTimeMillis();

	private int 心跳 = 10;

	private boolean isConnected = false;

	private Socket orgSocket;

	private String target;

	private Socket innerSocket;

	private String proxyHost;

	private int proxyPort;

	private final String TAG = "CMWRAP->WapChannel";

	private final String UA = "biAji's wap channel";

	public WapChannel(Socket socket) {
		this(socket, "10.0.0.172", 80);
	}

	public WapChannel(Socket socket, String proxyHost, int proxyPort) {
		this(socket, "android.clients.google.com:443", proxyHost, proxyPort);
	}

	public WapChannel(Socket socket, String target, String proxyHost,
			int proxyPort) {
		this.orgSocket = socket;
		this.target = target;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
	}

	private void buildProxy() {

		Log.d(TAG, "建立通道");
		DataInputStream din = null;
		DataOutputStream dout = null;

		try {
			innerSocket = new Socket(proxyHost, proxyPort);
			din = new DataInputStream(innerSocket.getInputStream());
			dout = new DataOutputStream(innerSocket.getOutputStream());

			String connectStr = "CONNECT " + target
					+ " HTTP/1.1\r\nUser-agent: " + this.UA + "\r\n\r\n";

			dout.writeBytes(connectStr);
			String result = "";
			String line = "";
			while ((line = din.readLine()) != null) {
				result += line;
			}
			// Log.d(TAG, connectStr);
			// Log.d(TAG, new String(result.getBytes("UTF-8")));

			if (result.contains("established")) {
				isConnected = true;
				Log.d(TAG, "通道建立成功");
			}

		} catch (UnknownHostException e) {
			Log.e(TAG, "无法获取代理服务器的IP地址", e);
		} catch (IOException e) {
			Log.e(TAG, "建立隧道失败", e);
		}
	}

	@Override
	public void run() {
		buildProxy();
		if (orgSocket != null && innerSocket != null && orgSocket.isConnected()
				&& innerSocket.isConnected()) {
			Pipe come = new Pipe(innerSocket, orgSocket, "↓");
			come.start();
			Pipe go = new Pipe(orgSocket, innerSocket, "↑");
			go.start();
		}
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	class Pipe extends Thread {
		Socket in = null, out = null;
		String direction = "";

		Pipe(Socket in, Socket out, String direction) {
			this.in = in;
			this.out = out;
			this.direction = direction;
		}

		@Override
		public void run() {
			Log.d(TAG, direction + "线程启动");
			int count = 0;
			DataInputStream sin = null;
			DataOutputStream dout = null;
			try {

				sin = new DataInputStream(in.getInputStream());

				dout = new DataOutputStream(out.getOutputStream());

				while (true) {

					byte[] buff = new byte[in.getReceiveBufferSize()];

					count = sin.read(buff);

					if (count > 0) {
						Log.d(TAG, "方向" + direction
								+ Utils.bytesToHexString(buff, 0, count));
						dout.write(buff, 0, count);
					} else if (count < 0) {
						break;
					}

				}
			} catch (SocketException e) {
				Log.e(TAG, "该死的Socket不老实了也", e);
			} catch (IOException e) {
				Log.e(TAG, "管道通讯失败", e);
			} finally {
			}
		}
	}

}
