package net.biaji.android.cmwrap.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

import net.biaji.android.cmwrap.Logger;

public class WapChannel extends Thread {

	private long starTime = System.currentTimeMillis();

	private boolean isConnected = false;

	private Socket orgSocket;

	private String target;

	private Socket innerSocket;

	private String proxyHost;

	private int proxyPort;

	private final String TAG = "CMWRAP->WapChannel";

	private final String UA = "biAji's wap channel";

	/**
	 * 
	 * @param socket
	 *            本地服务侦听接受的Socket
	 * @param proxyHost
	 *            代理服务器主机地址
	 * @param proxyPort
	 *            代理服务器端口
	 */
	public WapChannel(Socket socket, String proxyHost, int proxyPort) {
		this(socket, "android.clients.google.com:443", proxyHost, proxyPort);
	}

	/**
	 * 
	 * @param socket
	 *            本地服务侦听接受的Socket
	 * @param target
	 *            将要连接的目标地址，格式为 主机地址:端口号
	 * @param proxyHost
	 *            代理服务器主机地址
	 * @param proxyPort
	 *            代理服务器端口
	 */
	public WapChannel(Socket socket, String target, String proxyHost,
			int proxyPort) {
		this.orgSocket = socket;
		this.target = target;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;

		buildProxy();
	}

	/**
	 * 建立经由HTTP代理服务器链接至目的服务器的隧道
	 */
	private void buildProxy() {

		Logger.v(TAG, "建立通道");
		DataInputStream din = null;
		DataOutputStream dout = null;

		try {
			innerSocket = new Socket(proxyHost, proxyPort);
			innerSocket.setKeepAlive(true);
			innerSocket.setSoTimeout(120 * 1000);

			din = new DataInputStream(innerSocket.getInputStream());
			dout = new DataOutputStream(innerSocket.getOutputStream());

			String connectStr = "CONNECT " + target
					+ " HTTP/1.1\r\nUser-agent: " + this.UA + "\r\n\r\n";

			dout.writeBytes(connectStr);
			dout.flush();
			Logger.v(TAG, connectStr);

			String result = din.readLine();
			din.readLine(); // 多了个0D0A

			Logger.v(TAG, result);

			if (result != null && result.contains("200")) {
				isConnected = true;
				Logger.v(TAG, "通道建立成功， 耗时："
						+ (System.currentTimeMillis() - starTime) / 1000);
			}

		} catch (UnknownHostException e) {
			Logger.e(TAG, "无法获取代理服务器的IP地址", e);
		} catch (IOException e) {
			Logger.e(TAG, "建立隧道失败", e);
		}
	}

	@Override
	public void run() {
		if (orgSocket != null && innerSocket != null && orgSocket.isConnected()
				&& innerSocket.isConnected()) {
			DataInputStream oin, din;
			DataOutputStream oout, dout;
			try {
				oin = new DataInputStream(orgSocket.getInputStream());
				oout = new DataOutputStream(orgSocket.getOutputStream());

				din = new DataInputStream(innerSocket.getInputStream());
				dout = new DataOutputStream(innerSocket.getOutputStream());

				Pipe go = new Pipe(oin, dout, "↑");
				Pipe come = new Pipe(din, oout, "↓");
				go.start();
				come.start();

			} catch (IOException e) {
				Logger.e(TAG, "获取流失败：" + e.getLocalizedMessage());
			}
		}
	}

	public boolean isConnected() {

		if (System.currentTimeMillis() - starTime < 2000)
			return true;

		if (this.orgSocket == null && this.innerSocket.isConnected())
			return true;

		if (this.orgSocket != null && this.innerSocket.isConnected()
				&& this.orgSocket.isConnected()) {
			isConnected = true;
		}
		return isConnected;
	}

	public void destory() {
		if (orgSocket != null)
			clean(orgSocket);
		if (innerSocket != null)
			clean(innerSocket);
	}

	private void clean(Socket socket) {
		try {
			if (!socket.isClosed())
				socket.close();
		} catch (IOException e) {
			Logger.e(TAG, "销毁失败");
		} finally {
			try {
				socket.close();
			} catch (Exception e) {
			}
		}
	}

	class Pipe extends Thread {
		DataInputStream in = null;
		DataOutputStream out = null;
		String direction = "";

		Pipe(DataInputStream in, DataOutputStream out, String direction) {
			this.in = in;
			this.out = out;
			this.direction = direction;
		}

		@Override
		public void run() {
			Logger.v(TAG, direction + "线程启动");
			int count = 0;
			try {

				while (isConnected) {

					byte[] buff = new byte[1024];

					count = in.read(buff);

					if (count > 0) {
						// Log.d(TAG, "方向" + direction
						// + Utils.bytesToHexString(buff, 0, count));
						Logger.v(TAG, direction + "--" + count);
						out.write(buff, 0, count);
					} else if (count < 0) {
						break;
					}

				}
			} catch (IOException e) {
				Logger.e(TAG, direction + " 管道通讯失败", e);
				isConnected = false;
			}
		}
	}

}
