package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import android.util.Log;

public class WrapServer extends Thread {

	private ServerSocket serSocket;

	private int port;

	private final String TAG = "CMWRAP->Server";

	private boolean inService = false;

	public WrapServer(int port) {
		this.port = port;
		try {
			serSocket = new ServerSocket(port);
			Log.d(TAG, "服务在端口" + port + "上启动成功");
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

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		try {

			while (inService) {
				Log.d(TAG, "等待客户端请求……");
				Socket socket = serSocket.accept();
				Log.d(TAG, "获得客户端请求");
				WapChannel channel = new WapChannel(socket);
				channel.start();
			}
		} catch (IOException e) {
			Log.e(TAG, "folk channelThread failed", e);
		}

	}

}
