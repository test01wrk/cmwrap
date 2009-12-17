package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;

import net.biaji.android.cmwrap.Logger;

public abstract class WrapServer extends Thread {

	private ServerSocket serSocket;

	private int servPort;

	private String dest;

	private String name;

	private String proxyHost;

	private int proxyPort;

	private final String TAG = "CMWRAP->Server";

	private boolean inService = false;

	private HashSet<WapChannel> channels = new HashSet<WapChannel>();

	public boolean isClosed() {
		return serSocket.isClosed();
	}

	public void close() throws IOException {
		inService = false;
		serSocket.close(); // TODO 优雅点
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
	public void destroy() {
		Logger.v(TAG, name + "侦听服务被系统销毁");
		super.destroy();
	}


}
