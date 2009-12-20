package net.biaji.android.cmwrap.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

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

		byte[] qbuffer = new byte[576];
		DatagramPacket dnsq = new DatagramPacket(qbuffer, qbuffer.length);
		while (true) {
			try {

				srvSocket.receive(dnsq);
				byte[] dnsquest = dnsq.getData();

				// 连接外部DNS进行解析。
				new Thread() {
					Socket innerSocket;
					private final String UA = "biAji's wap channel";
					private long starTime = System.currentTimeMillis();
					private String target = "4.3.2.1";  //TODO 读取配置

					public void run() {
						DatagramPacket dnsAnswer = null;
						byte[] buff = new byte[1024];
						try {
							buildProxy();
							if(innerSocket.isConnected()){
								
							}
							srvSocket.send(dnsAnswer);
						} catch (IOException e) {
							Logger.e(TAG, "返回DNS解析结果错误", e);
						}
					}

					private void buildProxy() {

						Logger.v(TAG, "建立通道");
						BufferedReader din = null;
						BufferedWriter dout = null;

						try {
							innerSocket = new Socket(proxyHost, proxyPort);
							innerSocket.setKeepAlive(true);
							innerSocket.setSoTimeout(120 * 1000);

							din = new BufferedReader(new InputStreamReader(
									innerSocket.getInputStream()));
							dout = new BufferedWriter(new OutputStreamWriter(
									innerSocket.getOutputStream()));

							String connectStr = "CONNECT " + target + " HTTP/1.0\r\nUser-agent: "
									+ this.UA + "\r\n\r\n";

							dout.write(connectStr);
							dout.flush();
							Logger.v(TAG, connectStr);

							String result = din.readLine();
							String line = "";
							while ((line = din.readLine()) != null) {
								if (line.trim().equals(""))
									break;
								Logger.v(TAG, line);
							}

							if (result != null && result.contains("200")) {
								Logger.v(TAG, result);
								Logger
										.i(
												TAG,
												"通道建立成功， 耗时："
														+ (System
																.currentTimeMillis() - starTime)
														/ 1000);
							}

						} catch (UnknownHostException e) {
							Logger.e(TAG, "无法解析代理服务器地址："
									+ e.getLocalizedMessage());
						} catch (IOException e) {
							Logger.e(TAG, "建立隧道失败：" + e.getLocalizedMessage());
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
