/**
 * 
 */
package net.biaji.android.cmwrap.services;

import java.io.IOException;
import java.net.SocketException;

import net.biaji.android.cmwrap.Config;
import net.biaji.android.cmwrap.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

/**
 * @author yanghong
 * 
 */
public class DNSServerHttp extends DNSServer {

	private final String TAG = "DNSServerHttp";
	final private String CANT_RESOLVE = "-";
	final private int MAX_IP_LEN = 16;

	private DefaultHttpClient httpClient = null;

	public DNSServerHttp(String name, int port, String proxyHost,
			int proxyPort, String httpAPI, int httpPort) {
		/*
		 * Initial DNSServer with a default DNS server, the dnsHost address has
		 * been used for set net.dns1
		 */
		super(name, port, proxyHost, proxyPort, Config.DEFAULT_DNS_ADD, 53);

		/* Set it again while we actually use the httpAPI */
		this.dnsHost = httpAPI;
		this.dnsPort = httpPort;

		httpClient = new DefaultHttpClient();

		updateHttpProxySetting();
	}

	/*
	 * Implement with http based DNS.
	 * 
	 * @see net.biaji.android.cmwrap.services.DNSServer#fetchAnswer(byte[])
	 */

	@Override
	public byte[] fetchAnswer(byte[] quest) {
		byte[] result = null;
		String domain = getRequestDomain(quest);
		String ip = null;

		/* Not support reverse domain name query */
		if (domain.endsWith("in-addr.arpa")) {
			return null;
		}

		ip = resolveDomainName(domain);

		if (ip == null) {
			Logger.e(TAG, "Failed to resolve domain name: " + domain);
			return null;
		}

		/* FIXME: BJ's wap gateway return a wml page at first access */
		if (ip.startsWith("<?xml")) {
			Logger.d(TAG,
					"Malformed content, some wap gateway sucks, query again");
			ip = resolveDomainName(domain);
		}

		if (ip == CANT_RESOLVE) {
			return null;
		}

		byte[] ips = parseIPString(ip);
		if (ips != null) {
			result = createDNSResponse(quest, ips);
		}

		return result;
	}

	/*
	 * Resolve host name by access a DNSRelay running on GAE:
	 * 
	 * Example:
	 * 
	 * http://dn5r3l4y.appspot.com/?ww.woggoelc.mo (domain name encoded)
	 * 
	 * DNSRelay project:
	 * http://github.com/yangh/code-slices/tree/master/dnsrelay/
	 */
	private String resolveDomainName(String domain) {
		HttpEntity entity = null;
		String ip = null;
		byte[] ips = null;
		int len = 0;

		String uri = dnsHost + ":" + dnsPort + "/?" + shake(domain);
		HttpResponse response = null;
		HttpUriRequest request = null;

		try {
			request = new HttpGet(uri);
		} catch (IllegalArgumentException e) {
			Logger.e(TAG, "Failed to create request for URI: " + uri, e);
			return null;
		}

		Logger.d(TAG, "Query: " + uri);

		try {
			response = httpClient.execute(request);
			entity = response.getEntity();
			if (entity != null) {
				len = (int) entity.getContentLength();
				if (len < 0)
					len = 0;
				len = len < MAX_IP_LEN ? len : MAX_IP_LEN;
				ips = new byte[len];
				entity.getContent().read(ips, 0, len);
				ip = new String(ips);
			}
		} catch (ClientProtocolException e) {
			Logger.e(TAG, "Failed to request URI: " + uri, e);
		} catch (SocketException e) {
			Logger.e(TAG, "Failed to request URI: " + uri, e);
		} catch (IOException e) {
			Logger.e(TAG, "Failed to request URI: " + uri, e);
		} catch (NullPointerException e) {
			Logger.e(TAG, "Failed to request URI: " + uri, e);
		}

		return ip;
	}

	/*
	 * Switch char[n] and char[n+1] one by one, for Fucking GFW.
	 * 
	 * example: www.google.com ->ww.woggoelc.mo
	 */
	private String shake(String src) {
		int i, n;
		byte[] ret = null;
		byte[] str = null;
		String shaked = null;

		if (src.length() == 0) {
			return null;
		}

		str = src.getBytes();
		ret = new byte[str.length];

		i = n = 0;
		while (n < str.length / 2) {
			ret[i] = str[i + 1];
			ret[i + 1] = str[i];
			i += 2;
			n++;
		}

		if (str.length % 2 == 1) {
			ret[str.length - 1] = str[str.length - 1];
		}

		shaked = new String(ret);
		Logger.d(TAG, "Shaked domain name: " + shaked);

		return shaked;
	}

	@Override
	public void setProxyHost(String host) {
		this.proxyHost = host;

		updateHttpProxySetting();
	}

	@Override
	public void setProxyPort(int port) {
		this.proxyPort = port;

		updateHttpProxySetting();
	}

	private void updateHttpProxySetting() {
		HttpHost proxy = null;
		HttpParams params = null;

		proxy = new HttpHost(proxyHost, proxyPort);
		params = httpClient.getParams();

		if (params != null) {
			params.setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
		}
	}
	
	@Override
	public boolean test(String domain, String ip) {
		String resolved = null;
		
		resolved = resolveDomainName(domain);
		return ip == resolved;
	}

}
