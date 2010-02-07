package net.biaji.android.cmwrap.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.biaji.android.cmwrap.Logger;

public class NormalTcpServer extends WrapServer {

	private ServerSocket serSocket;

	private int servPort;

	private String target;

	private String name;

	private String proxyHost;

	private int proxyPort;

	private final String TAG = "CMWRAP->TCPServer";

	private boolean inService = false;

	private ExecutorService serv = Executors.newCachedThreadPool();

	private Hashtable<String, String> connReq = new Hashtable<String, String>();

	public NormalTcpServer(String name, int port) {
		this(name, port, "10.0.0.172", 80);
	}

	/**
	 * 
	 * @param name
	 *            服务名称
	 * @param port
	 *            侦听端口号
	 * @param proxyHost
	 *            HTTP代理服务器地址
	 * @param proxyPort
	 *            HTTP代理服务器端口
	 */
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

	public void close() {
		inService = false;
		serv.shutdownNow();
		try {
			serSocket.close();
		} catch (IOException e) {
			Logger.e(TAG, "", e);
		}
	}

	public int getServPort() {
		return servPort;
	}

	/**
	 * 设置此服务的目的地址
	 * 
	 * @param dest
	 */
	public void setTarget(String dest) {
		this.target = dest;
	}

	@Override
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;

	}

	@Override
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;

	}

	@Override
	public void run() {

		while (inService) {
			try {
				Logger.v(TAG, "等待客户端请求……");
				Socket socket = serSocket.accept();
				socket.setSoTimeout(120 * 1000);
				Logger.v(TAG, "获得客户端请求");

				String srcPort = socket.getLocalPort() + "";
				target = getTarget(srcPort);
				if (target == null || target.trim().equals("")) {
					Logger.d(TAG, "源于端口号" + srcPort + "的链接未匹配。");
					continue;
				} else {
					Logger.d(TAG, srcPort + "匹配至目的地址：" + target);
				}
				serv.execute(new WapChannel(socket, target, proxyHost,
						proxyPort));

				TimeUnit.MILLISECONDS.sleep(100);
			} catch (IOException e) {
				Logger.e(TAG, "伺服客户请求失败" + e.getMessage());
			} catch (InterruptedException e) {
				Logger.e(TAG, "Interrupted:" + e.getMessage());
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
		close();
		super.destroy();
	}

	/**
	 * 根据源端口号，由dmesg找出iptables记录的目的地址
	 * 
	 * @param sourcePort
	 *            连接源端口号
	 * @return 目的地址，形式为 addr:port
	 */
	private synchronized String getTarget(String sourcePort) {
		String result = "";

		// 在表中查找已匹配项目
		if (connReq.containsKey(sourcePort)) {
			result = connReq.get(sourcePort);
			connReq.remove(sourcePort);
			return result;
		}

		final String command = "dmesg -c"; // 副作用未知

		DataOutputStream os = null;
		InputStream out = null;
		try {
			Process process = Runtime.getRuntime().exec("su");

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(command + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();

			int execResult = process.waitFor();
			if (execResult == 0)
				Logger.d(TAG, command + " exec success");
			else {
				Logger.d(TAG, command + " exec with result " + execResult);
			}

			out = process.getInputStream();
			BufferedReader outR = new BufferedReader(new InputStreamReader(out));
			String line = "";

			// 根据输出构建以源端口为key的地址表
			while ((line = outR.readLine()) != null) {

				boolean match = false;

				if (line.contains("CMWRAP")) {
					String addr = "", destPort = "";
					String[] parmArr = line.split(" ");
					for (String parm : parmArr) {
						String trimParm = parm.trim();
						if (trimParm.startsWith("DST")) {
							addr = getValue(trimParm);
							break;
						}

						if (trimParm.startsWith("SPT")) {
							if (sourcePort.equals(getValue(trimParm)))
								match = true;
							break;
						}

						if (trimParm.startsWith("DPT")) {
							destPort = getValue(trimParm);
							break;
						}

					}

					if (match)
						result = addr + ":" + destPort;
					else
						connReq.put(addr, destPort);

				}
			}

			os.close();
			process.destroy();
		} catch (IOException e) {
			Logger.e(TAG, "Failed to exec command", e);
		} catch (InterruptedException e) {
			Logger.e(TAG, "线程意外终止", e);
		} finally {
			try {
				if (os != null) {
					os.close();
				}
			} catch (IOException e) {
			}

		}

		return result;
	}

	private String getValue(String org) {
		String result = "";
		result = org.substring(org.indexOf("=") + 1);
		return result;
	}

}
