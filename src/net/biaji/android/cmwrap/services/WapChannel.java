package net.biaji.android.cmwrap.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.UnknownHostException;

import net.biaji.android.cmwrap.Utils;
import android.util.Log;

public class WapChannel extends Thread {

	private long starTime = System.currentTimeMillis();

	//private int 心跳 = 10;

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

	public WapChannel(Socket socket, String target) {
		this(socket, target, "10.0.0.172", 80);
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
			dout.flush();
			Log.d(TAG, connectStr + (System.currentTimeMillis() - starTime)
					/ 1000);
			String result = din.readLine();
			din.readLine(); //多了个0D0A
			// String line = "";
			// while ((line = din.readLine()) != null) {
			// result += line;
			// }

			Log.d(TAG, result + (System.currentTimeMillis() - starTime) / 1000);

			if (result != null && result.contains("established")) {
				isConnected = true;
				Log.d(TAG, "通道建立成功， 耗时："
						+ (System.currentTimeMillis() - starTime) / 1000);
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
			DataInputStream oin, din;
			DataOutputStream oout, dout;
			try {
				oin = new DataInputStream(orgSocket.getInputStream());
				oout = new DataOutputStream(orgSocket.getOutputStream());

				din = new DataInputStream(innerSocket.getInputStream());
				dout = new DataOutputStream(innerSocket.getOutputStream());

				while (true) {

					byte[] buff = new byte[1024];
					int count = 0;
					try {
						if ((count = oin.read(buff)) > 0) {
//							Log.d(TAG, "↑"
//									+ Utils.bytesToHexString(buff, 0, count));
							Log.d(TAG, "↑" + count);
							dout.write(buff, 0, count);
							dout.flush();
						} else if (count < 0) {
							break;
						}
					} catch (InterruptedIOException e) {
					}
					Thread.sleep(1500);
					try {
						if ((count = din.read(buff)) > 0) {
//							 Log.d(TAG, "↓"
//									+ Utils.bytesToHexString(buff, 0, count));
							Log.d(TAG, "↓" + count);
							oout.write(buff, 0, count);
						} else if (count < 0) {
							break;
						}
					} catch (InterruptedIOException e) {
					}
				}

				// Pipe go = new Pipe(oin, dout, "↑");
				// Pipe come = new Pipe(din, oout, "↓");
				// go.start();
				// come.start();

			} catch (IOException e) {
				Log.e(TAG, "获取流失败：" + e.getLocalizedMessage());
			} catch (InterruptedException e) {
				Log.e(TAG, "忙的一塌糊涂", e);
			}
		}
	}

	public boolean isConnected() {

		if (System.currentTimeMillis() - starTime < 1000)
			return true;

		if (this.innerSocket.isConnected() && this.orgSocket.isConnected()) {
			isConnected = true;
		}
		return isConnected;
	}

	public void destory() {
		clean(orgSocket);
		clean(innerSocket);
	}

	private void clean(Socket socket) {
		try {
			if (!socket.isClosed())
				socket.close();
		} catch (IOException e) {
			Log.e(TAG, "销毁失败");
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}
	//
	// class Pipe extends Thread {
	// DataInputStream in = null;
	// DataOutputStream out = null;
	// String direction = "";
	//
	// Pipe(DataInputStream in, DataOutputStream out, String direction) {
	// this.in = in;
	// this.out = out;
	// this.direction = direction;
	// }
	//
	// @Override
	// public void run() {
	// Log.d(TAG, direction + "线程启动");
	// int count = 0;
	// try {
	//
	// while (true) {
	//
	// byte[] buff = new byte[1024 * 8];
	//
	// count = in.read(buff);
	//
	// if (count > 0) {
	// Log.d(TAG, "方向" + direction
	// + Utils.bytesToHexString(buff, 0, count));
	// out.write(buff, 0, count);
	// } else if (count < 0) {
	// break;
	// }
	//
	// }
	// } catch (SocketException e) {
	// Log.e(TAG, "该死的Socket不老实了也", e);
	// } catch (IOException e) {
	// Log.e(TAG, "管道通讯失败", e);
	// }
	// }
	// }

}
