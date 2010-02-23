package net.biaji.android.cmwrap.services;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.concurrent.TimeUnit;

import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.utils.Utils;

/**
 * 此类实现了DNS代理
 * 
 * @author biaji
 * 
 */
public class DNSServer implements WrapServer {

	private final String TAG = "CMWRAP->DNSServer";

	private String homePath;
	private final String CACHE_PATH = "/cache";
	private final String CACHE_FILE = "/dnscache";

	private DatagramSocket srvSocket;

	private int srvPort;

	private String name;
	private String proxyHost;
	private int proxyPort;

	private boolean inService = false;

	private Hashtable<String, DnsResponse> dnsCache = new Hashtable<String, DnsResponse>();

	private String target = "8.8.8.8:53";

	public DNSServer(String name, int port, String proxyHost, int proxyPort) {
		try {

			this.name = name;
			this.srvPort = port;
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;

			srvSocket = new DatagramSocket(srvPort, InetAddress
					.getByName("127.0.0.1"));
			inService = true;

			Logger.d(TAG, this.name + "启动于端口： " + port);

		} catch (SocketException e) {
			Logger.e(TAG, "DNSServer初始化错误，端口号" + port, e);
		} catch (UnknownHostException e) {
			Logger.e(TAG, "DNSServer初始化错误，端口号" + port, e);
		}

	}

	public void run() {

		loadCache();

		byte[] qbuffer = new byte[576];
		long starTime = System.currentTimeMillis();
		while (true) {
			try {
				DatagramPacket dnsq = new DatagramPacket(qbuffer,
						qbuffer.length);

				srvSocket.receive(dnsq);
				// 连接外部DNS进行解析。

				byte[] data = dnsq.getData();
				int dnsqLength = dnsq.getLength();
				byte[] udpreq = new byte[dnsqLength];
				System.arraycopy(data, 0, udpreq, 0, dnsqLength);
				// 尝试从缓存读取域名解析
				String questDomain = getRequestDomain(udpreq);

				Logger.d(TAG, "解析" + questDomain);

				if (dnsCache.containsKey(questDomain)) {

					sendDns(dnsCache.get(questDomain).getDnsResponse(), dnsq,
							srvSocket);

					Logger.d(TAG, "命中缓存");

				} else {
					starTime = System.currentTimeMillis();
					byte[] answer = fetchAnswer(udpreq);
					if (answer != null && answer.length != 0) {
						DnsResponse response = new DnsResponse(questDomain);
						response.setDnsResponse(answer);
						dnsCache.put(questDomain, response);
						sendDns(answer, dnsq, srvSocket);
						saveCache();
						Logger.d(TAG, "正确返回DNS解析，长度："
								+ response.getDnsResponse().length + "  耗时："
								+ (System.currentTimeMillis() - starTime)
								/ 1000 + "s");
					} else {
						Logger.e(TAG, "返回DNS包长为0");
					}

				}

			} catch (SocketException e) {
				Logger.e(TAG, e.getLocalizedMessage());
				break;
			} catch (IOException e) {
				Logger.e(TAG, e.getLocalizedMessage());
			}
		}

	}

	/**
	 * 由上级DNS通过TCP取得解析
	 * 
	 * @param quest
	 * @return
	 */
	private byte[] fetchAnswer(byte[] quest) {

		Socket innerSocket = new InnerSocketBuilder(proxyHost, proxyPort,
				target).getSocket();
		DataInputStream in;
		DataOutputStream out;
		byte[] result = null;
		try {
			if (innerSocket != null && innerSocket.isConnected()) {
				// 构造TCP DNS包
				int dnsqLength = quest.length;
				byte[] tcpdnsq = new byte[dnsqLength + 2];
				System.arraycopy(Utils.int2byte(dnsqLength), 0, tcpdnsq, 1, 1);
				System.arraycopy(quest, 0, tcpdnsq, 2, dnsqLength);

				// 转发DNS
				in = new DataInputStream(innerSocket.getInputStream());
				out = new DataOutputStream(innerSocket.getOutputStream());
				out.write(tcpdnsq);
				out.flush();

				ByteArrayOutputStream bout = new ByteArrayOutputStream();

				int b = -1;
				while ((b = in.read()) != -1) {
					bout.write(b);
				}
				byte[] tcpdnsr = bout.toByteArray();
				if (tcpdnsr != null && tcpdnsr.length > 2) {
					result = new byte[tcpdnsr.length - 2];
					System.arraycopy(tcpdnsr, 2, result, 0, tcpdnsr.length - 2);
				}
				innerSocket.close();
			}
		} catch (IOException e) {
			Logger.e(TAG, "", e);
		}
		return result;
	}

	/**
	 * 向来源发送dns应答
	 * 
	 * @param response
	 *            应答包
	 * @param dnsq
	 *            请求包
	 * @param srvSocket
	 *            侦听Socket
	 */
	private void sendDns(byte[] response, DatagramPacket dnsq,
			DatagramSocket srvSocket) {

		// 同步identifier
		System.arraycopy(dnsq.getData(), 0, response, 0, 2);

		DatagramPacket resp = new DatagramPacket(response, 0, response.length);
		resp.setPort(dnsq.getPort());
		resp.setAddress(dnsq.getAddress());

		try {
			srvSocket.send(resp);
		} catch (IOException e) {
			Logger.e(TAG, "", e);
		}
	}

	/**
	 * 获取UDP DNS请求的域名
	 * 
	 * @param request
	 *            dns udp包
	 * @return 请求的域名
	 */
	private String getRequestDomain(byte[] request) {
		String requestDomain = "";
		int reqLength = request.length;
		if (reqLength > 13) { // 包含包体
			byte[] question = new byte[reqLength - 12];
			System.arraycopy(request, 12, question, 0, reqLength - 12);
			requestDomain = getPartialDomain(question);
			requestDomain = requestDomain.substring(0,
					requestDomain.length() - 1);
		}
		return requestDomain;
	}

	/**
	 * 解析域名
	 * 
	 * @param request
	 * @return
	 */
	private String getPartialDomain(byte[] request) {

		String result = "";
		int length = request.length;
		int partLength = request[0];
		if (partLength == 0)
			return result;
		try {
			byte[] left = new byte[length - partLength - 1];
			System.arraycopy(request, partLength + 1, left, 0, length
					- partLength - 1);
			result = new String(request, 1, partLength) + ".";
			result += getPartialDomain(left);
		} catch (Exception e) {
			Logger.e(TAG, e.getLocalizedMessage());
		}
		return result;
	}

	/**
	 * 由缓存载入域名解析缓存
	 */
	private void loadCache() {
		ObjectInputStream ois = null;
		File cache = new File(homePath + CACHE_PATH + CACHE_FILE);
		try {
			if (!cache.exists())
				return;
			ois = new ObjectInputStream(new FileInputStream(cache));
			dnsCache = (Hashtable<String, DnsResponse>) ois.readObject();
			ois.close();
			ois = null;

			for (DnsResponse resp : dnsCache.values()) {
				// 检查缓存时效(十天)
				if ((System.currentTimeMillis() - resp.getTimestamp()) > 864000000L)
					dnsCache.remove(resp.getRequest());
			}

		} catch (ClassCastException e) {
			Logger.e(TAG, e.getLocalizedMessage(), e);
		} catch (FileNotFoundException e) {
			Logger.e(TAG, e.getLocalizedMessage(), e);
		} catch (IOException e) {
			Logger.e(TAG, e.getLocalizedMessage(), e);
		} catch (ClassNotFoundException e) {
			Logger.e(TAG, e.getLocalizedMessage(), e);
		} finally {
			try {
				if (ois != null)
					ois.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * 保存域名解析内容缓存
	 */
	private void saveCache() {
		ObjectOutputStream oos = null;
		File cache = new File(homePath + CACHE_PATH + CACHE_FILE);
		try {
			if (!cache.exists()) {
				File cacheDir = new File(homePath + CACHE_PATH);
				if (!cacheDir.exists()) { // android的createNewFile这个方法真够恶心的啊
					cacheDir.mkdir();
				}
				cache.createNewFile();
			}
			oos = new ObjectOutputStream(new FileOutputStream(cache));
			oos.writeObject(dnsCache);
			oos.flush();
			oos.close();
			oos = null;
		} catch (FileNotFoundException e) {
			Logger.e(TAG, e.getLocalizedMessage(), e);
		} catch (IOException e) {
			Logger.e(TAG, e.getLocalizedMessage(), e);
		} finally {
			try {
				if (oos != null)
					oos.close();
			} catch (IOException e) {
			}
		}
	}

	public void close() throws IOException {
		inService = false;
		srvSocket.close();
		saveCache();
		Logger.i(TAG, "DNS服务关闭");
	}

	public int getServPort() {
		return this.srvPort;
	}

	public boolean isClosed() {
		return srvSocket.isClosed();
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public void setProxyHost(String host) {
		this.proxyHost = host;
	}

	public void setProxyPort(int port) {
		this.proxyPort = port;

	}

	public void setBasePath(String path) {
		this.homePath = path;
	}

}

class DnsResponse implements Serializable {

	private static final long serialVersionUID = -6693216674221293274L;

	private String request;
	private long timestamp = System.currentTimeMillis();;
	private int reqTimes;
	private byte[] dnsResponse;

	public DnsResponse(String request) {
		this.request = request;
	}

	public String getRequest() {
		return this.request;
	}

	/**
	 * @return the timestamp
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * @return the reqTimes
	 */
	public int getReqTimes() {
		return reqTimes;
	}

	/**
	 * @return the dnsResponse
	 */
	public byte[] getDnsResponse() {
		this.reqTimes++;
		// this.timestamp = System.currentTimeMillis();
		return dnsResponse;
	}

	/**
	 * @param dnsResponse
	 *            the dnsResponse to set
	 */
	public void setDnsResponse(byte[] dnsResponse) {
		this.dnsResponse = dnsResponse;
	}

}
