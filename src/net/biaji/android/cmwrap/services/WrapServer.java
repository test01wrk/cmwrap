package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;

import net.biaji.android.cmwrap.Cmwrap;

import android.util.Log;

public class WrapServer extends Thread {

	private ServerSocket serSocket;

	private int servPort;

	private String dest;

	private final String TAG = "CMWRAP->Server";

	private boolean inService = false;

	private HashSet<WapChannel> channels = new HashSet<WapChannel>();

	public WrapServer(int port) {
		this.servPort = port;
		try {
			serSocket = new ServerSocket(port);
			inService = true;
		} catch (IOException e) {
			Log.e(TAG, "Server初始化错误，端口号" + port, e);
		}
	}

	public boolean isClosed() {
		return serSocket.isClosed();
	}

	public void close() throws IOException {
		inService = false;
		serSocket.close();
	}

	public int getServPort() {
		return servPort;
	}

	public void setServPort(int port) {
		this.servPort = port;
	}

	/**
	 * 设置此服务的目的地址
	 * 
	 * @param dest
	 */
	public void setDest(String dest) {
		this.dest = dest;
	}

	@Override
	public void run() {

		while (inService) {
			try {
				clean();
				Log.v(TAG, "等待客户端请求……");
				Socket socket = serSocket.accept();
				Log.v(TAG, "获得客户端请求");
				WapChannel channel = new WapChannel(socket, dest,
						Cmwrap.proxyHost, Cmwrap.proxyPort);
				channel.start();
				channels.add(channel);
				Thread.sleep(100);
			} catch (IOException e) {
				Log.e(TAG, "伺服客户请求失败", e);
			} catch (InterruptedException e) {
				Log.e(TAG, "世上本无事", e);
			}
		}

	}

	private void clean() {
		for (WapChannel channel : channels) {
			if (channel != null && !channel.isConnected()) {
				channel.destory();
				channels.remove(channel);
				Log.v(TAG, "清理链接");
			} else if (channel == null) {
				channels.remove(channel);
				Log.v(TAG, "清理无效链接");
			}
		}
	}

}
