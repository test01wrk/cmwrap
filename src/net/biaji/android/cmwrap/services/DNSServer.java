package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;

import net.biaji.android.cmwrap.Logger;

public class DNSServer extends WrapServer {

	private DatagramSocket srvSocket;

	private int srvPort;

	private String name;
	private String proxyHost;
	private int proxyPort;

	private final String TAG = "CMWRAP->DNSServer";

	private boolean inService = false;

	public DNSServer(String name, int port) {
		this(name, port, "10.0.0.172", 80);
	}

	public DNSServer(String name, int port, String proxyHost, int proxyPort) {
		try {

			this.name = name;
			this.srvPort = port;
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;

			srvSocket = new DatagramSocket(srvPort);
			inService = true;
			Logger.i(TAG, "DNSServer启动于端口： " + port);
		} catch (SocketException e) {
			Logger.e(TAG, "DNSServer初始化错误，端口号" + port, e);
		}

	}

	@Override
	public void run() {
		Socket innerSocket;
		byte[] qbuffer = new byte[576];
		DatagramPacket dnsq = new DatagramPacket(qbuffer, qbuffer.length);
		while (true) {
			try {
				
				srvSocket.receive(dnsq);
				byte[] dnsquest = dnsq.getData();
				
				//连接外部DNS进行解析。
				new Thread(){
					public void run() {
						DatagramPacket dnsAnswer = null;
						try {
							srvSocket.send(dnsAnswer);
						} catch (IOException e) {
							Logger.e(TAG, "返回DNS解析结果错误", e);
						}
					}
					
				}.start();

			} catch (IOException e) {
				Logger.e(TAG, "", e);
			}
		}

	}

	@Override
	public void close() throws IOException {
		inService = false;
		srvSocket.close();
	}

	@Override
	public int getServPort() {
		return this.srvPort;
	}

	@Override
	public boolean isClosed() {
		return srvSocket.isClosed();
	}

	@Override
	public void setDest(String dest) {
		// null
	}

	@Override
	public void setProxyHost(String host) {
		this.proxyHost = host;
	}

	@Override
	public void setProxyPort(int port) {
		this.proxyPort = port;

	}

}
