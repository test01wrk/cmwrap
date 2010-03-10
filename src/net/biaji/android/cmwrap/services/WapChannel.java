package net.biaji.android.cmwrap.services;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import net.biaji.android.cmwrap.Logger;

public class WapChannel implements Runnable {

	private long starTime = System.currentTimeMillis();

	private boolean isConnected = false;

	private Socket orgSocket;

	private Socket innerSocket;

	private final String TAG = "CMWRAP->WapChannel";

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

		InnerSocketBuilder builder = new InnerSocketBuilder(proxyHost,
				proxyPort, target);
		this.innerSocket = builder.getSocket();
		if (innerSocket != null && innerSocket.isConnected()
				&& builder.isConnected())
			this.isConnected = true;
	}

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

		if (System.currentTimeMillis() - starTime < 2000) {
			Logger.v(TAG, "建立时间少于2秒");
			return true;
		}

		if (this.orgSocket == null && this.innerSocket != null
				&& this.innerSocket.isConnected() && isConnected) {
			Logger.v(TAG, "测试用条件");
			return true;
		}

		if (this.innerSocket == null) {
			Logger.v(TAG, "代理不可及");
			isConnected = false;
		}

		Logger.d(TAG, "目前状态:" + isConnected);

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

	/**
	 * 经由HTTP代理建立的管道
	 * 
	 * @author biaji
	 * 
	 */
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
						// Logger.v(TAG, "方向" + direction
						// + Utils.bytesToHexString(buff, 0, count));
						Logger.v(TAG, orgSocket.getPort() + ": " + direction
								+ "--" + count);
						out.write(buff, 0, count);
					} else if (count < 0) {
						break;
					}

				}
			} catch (IOException e) {

				Logger.e(TAG, direction
						+ (System.currentTimeMillis() - starTime) / 1000
						+ " 管道通讯失败：" + e.getLocalizedMessage());
				if (orgSocket != null && !orgSocket.isClosed()) {
					try {
						Logger.d(TAG, orgSocket.getPort() + " closed!");
						orgSocket.close();
						orgSocket = null;
					} catch (Exception e2) {
						Logger.e(TAG, "", e2);
					}
				}

				isConnected = false;
			}
		}
	}

}
