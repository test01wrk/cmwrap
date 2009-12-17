package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;

import net.biaji.android.cmwrap.Logger;

public class NormalTcpServer extends WrapServer {
	private ServerSocket serSocket;

	private int servPort;

	private String dest;

	private String name;

	private String proxyHost;

	private int proxyPort;

	private final String TAG = "CMWRAP->TCPServer";

	private boolean inService = false;

	private HashSet<WapChannel> channels = new HashSet<WapChannel>();

	public NormalTcpServer(String name, int port, String proxyHost,
			int proxyPort) {
		this.servPort = port;
		this.name = name;
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		try {
			serSocket = new ServerSocket(port);
			inService = true;
		} catch (IOException e) {
			Logger.e(TAG, "Server初始化错误，端口号" + port, e);
		}
		Logger.d(TAG, "启用" + name + "服务于" + servPort + "端口");

	}

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
	public void run() {

		while (inService) {
			try {
				clean();
				Logger.v(TAG, "等待客户端请求……");
				Socket socket = serSocket.accept();
				socket.setSoTimeout(120 * 1000);
				Logger.v(TAG, "获得客户端请求");
				WapChannel channel = new WapChannel(socket, dest, proxyHost,
						proxyPort);
				if (channel.isConnected()) {
					channel.start();
					channels.add(channel);
				} else {
					channel.destory();
				}

				Thread.sleep(100);
			} catch (IOException e) {
				Logger.e(TAG, "伺服客户请求失败" + e.getMessage());
			} catch (InterruptedException e) {
				Logger.e(TAG, "世上本无事" + e.getMessage());
			}
		}

		try {
			serSocket.close();
		} catch (IOException e) {
			Logger.e(TAG, name + "关闭服务端口失败：" + e.getMessage());
		}
		Logger.v(TAG, name + "侦听服务停止");

	}

	@Override
	public void destroy() {
		Logger.v(TAG, name + "侦听服务被系统销毁");
		super.destroy();
	}

	private void clean() {

		for (Iterator<WapChannel> it = channels.iterator(); it.hasNext();) {
			WapChannel channel = it.next();
			if (channel != null && !channel.isConnected()) {
				channel.destory();
				it.remove();
				channels.remove(channel);
				Logger.d(TAG, name + "清理链接");
			} else if (channel == null) {
				it.remove();
				channels.remove(channel);
				Logger.d(TAG, name + "清理无效链接");
			}
		}

		Logger.d(TAG, name + " " + channels.size() + " channel 未清理");
	}

}
