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
		this(socket, "mtalk.google.com:5228", proxyHost, proxyPort);
	}

	public WapChannel(Socket socket, String target, String proxyHost,
			int proxyPort) {
		this.orgSocket = socket;
		this.target = target;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		buildProxy();
	}

	private void buildProxy() {
		DataInputStream din = null;
		DataOutputStream dout = null;

		try {
			innerSocket = new Socket(proxyHost, proxyPort);
			din = new DataInputStream(innerSocket.getInputStream());
			dout = new DataOutputStream(innerSocket.getOutputStream());

			String connectStr = "CONNECT " + target
					+ " HTTP/1.1\r\nUser-agent: " + this.UA + "\r\n\r\n";

			dout.writeChars(connectStr);
			String result = "";
			String line = "";
			while ((line = din.readLine()) != null) {
				result += line;
			}
			Log.d(TAG, result);
			if (result.contains("200"))
				isConnected = true;
			din.close();
			dout.close();
		} catch (UnknownHostException e) {
			Log.e(TAG, "无法获取代理服务器的IP地址", e);
		} catch (IOException e) {
			Log.e(TAG, "建立隧道失败", e);
		} finally {
			if (din != null) {
				try {
					din.close();
				} catch (IOException e) {
				}
			}
			if (dout != null) {
				try {
					dout.close();
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public void run() {
		if (orgSocket != null && innerSocket != null && orgSocket.isConnected()
				&& innerSocket.isConnected()) {
			Pipe go = new Pipe(orgSocket, innerSocket);
			go.start();
			Pipe come = new Pipe(innerSocket, orgSocket);
			come.start();
		}
	}

	public boolean isConnected() {
		return this.isConnected;
	}

	class Pipe extends Thread {
		Socket in = null, out = null;

		Pipe(Socket in, Socket out) {
			this.in = in;
			this.out = out;
		}

		@Override
		public void run() {

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
						dout.write(buff, 0, count);
					} else if (count < 0) {
						break;
					}

				}
				sin.close();
				dout.close();
			} catch (SocketException e) {
				Log.e(TAG, "该死的Socket不老实了也", e);
			} catch (IOException e) {
				Log.e(TAG, "管道通讯失败", e);
			} finally {

			}
		}
	}

}
