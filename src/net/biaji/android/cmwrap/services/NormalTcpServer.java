package net.biaji.android.cmwrap.services;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import net.biaji.android.cmwrap.Logger;

public class NormalTcpServer extends WrapServer {

	private final String LINE_SEPARATOR = System.getProperty("line.separator");

	private ServerSocket serSocket;

	private int servPort;

	private String dest;

	private String name;

	private String proxyHost;

	private int proxyPort;

	private final String TAG = "CMWRAP->TCPServer";

	private boolean inService = false;

	private ExecutorService serv = Executors.newCachedThreadPool();

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

	public void close() throws IOException {
		inService = false;
		serSocket.close();
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
		this.dest = dest;
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
				clean();
				Logger.v(TAG, "等待客户端请求……");
				Socket socket = serSocket.accept();
				socket.setSoTimeout(120 * 1000);
				Logger.v(TAG, "获得客户端请求");

				Future<?> f = serv.submit(new WapChannel(socket, dest,
						proxyHost, proxyPort));

				TimeUnit.MILLISECONDS.sleep(100);
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
		serv.shutdownNow();
	}

	private synchronized String rootCMD(String cmd) {
		String result = "";
		DataOutputStream os = null;
		InputStream out = null;
		try {
			Process process = Runtime.getRuntime().exec("su");

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(cmd + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();

			int execResult = process.waitFor();
			if (execResult == 0)
				Logger.d(TAG, cmd + " exec success");
			else {
				Logger.d(TAG, cmd + " exec with result " + execResult);
			}

			out = process.getInputStream();
			BufferedReader outR = new BufferedReader(new InputStreamReader(out));
			String line = "";
			StringBuilder outBuilder = new StringBuilder();
			while ((line = outR.readLine()) != null)
				outBuilder.append(line + LINE_SEPARATOR);

			result = outBuilder.toString();

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

}
