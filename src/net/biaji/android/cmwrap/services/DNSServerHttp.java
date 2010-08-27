/**
 * 
 */
package net.biaji.android.cmwrap.services;

import java.io.IOException;

import net.biaji.android.cmwrap.Logger;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

/**
 * @author yanghong
 *
 */
public class DNSServerHttp extends DNSServer {
	
	private final String TAG = "CMWRAP->DNSServerHttp";
	private final String CANT_RESOLVE = "-";
	final private int MAX_IP_LEN = 16;

	public DNSServerHttp(String name, int port, String httpAPI, int httpPort) {
		super(name, port, httpAPI, httpPort, "", 53);
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
		String ips = null;
		
		ips = resolveDomainName(domain);
		
		/* FIXME: BJ's wap gateway return a wml page at first access */
		if (ips.startsWith("<?xml")) {
			Logger.d(TAG, "Malformed content, some wap gateway sucks, query again");
			ips = resolveDomainName(domain);
		}
		
		if (ips != CANT_RESOLVE) {
			result = createDNSResponse(quest, ips);
		}
		
		return result;
	}
	
	/*
	 * Resolve host name by access a DNSRelay running on GAE:
	 * 
	 * Example:
	 * 
	 *  http://dn5r3l4y.appspot.com/?ww.woggoelc.mo (domain name encoded)
	 * 
	 *  DNSRelay project:
	 *     http://github.com/yangh/code-slices/tree/master/dnsrelay/
	 */
	private String resolveDomainName(String domain) {
		HttpEntity entity = null;
		String ip = null;
		byte[] ips = null;
		int len = 0;

		String uri = proxyHost + ":" + "/?" + shake(domain);
		HttpResponse response = null;
		HttpUriRequest request = new HttpGet(uri);
		DefaultHttpClient httpClient = new DefaultHttpClient();

		try {
			response = httpClient.execute(request);
			entity = response.getEntity();
			if (entity != null) {
				len = (int) entity.getContentLength();
				len = len < MAX_IP_LEN ? len : MAX_IP_LEN;
				ips = new byte[len];
				entity.getContent().read(ips, 0, len);
				ip = new String(ips);
			}
		} catch (IOException e) {
			Logger.e(TAG, "Failed to request URI: " + uri);
		}
		
		return ip;
	}
	
	/*
	 * Switch char[n] and char[n+1] one by one, for Fucking GFW.
	 * 
	 * example:
	 * 		www.google.com
	 *    ->ww.woggoelc.mo
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
	
	/*
	 * Create a DNS response packet, which will send back to application.
	 * 
	 * Reference to: 
	 * 
	 *  Mini Fake DNS server (Python) 
	 *    http://code.activestate.com/recipes/491264-mini-fake-dns-server/
	 * 
	 *  DOMAIN NAMES - IMPLEMENTATION AND SPECIFICATION
	 *    http://www.ietf.org/rfc/rfc1035.txt
	 */
	private byte[] createDNSResponse (byte[] quest, String ip) {
		byte[] response = null;
		int[] b1 = {
				0, 0, 
				0x81, 0x80,
				0, 0, 0, 0,
				0, 0, 0, 0
				};
		int[] b2 = {
				0xc0, 0x0c,
				0x00, 0x01, 0x00, 0x01,0x00,
				0x00, 0x00, 0x3c, 0x00, 0x04
				};
		int start = 0;
		
		response = new byte[128];

		for (int val: b1) {
			response[start] = (byte) val;
			start++;
		}
		
		System.arraycopy(quest, 0, response, 0, 2); /* 0:2 */
		System.arraycopy(quest, 4, response, 4, 2); /* 4:6 -> 4:6 */
		System.arraycopy(quest, 4, response, 6, 2); /* 4:6 -> 7:9 */
		
		System.arraycopy(quest, 12, response, start, quest.length - 12); /* 12: -> 15: */
		start += quest.length - 12;

		for (int val: b2) {
			response[start] = (byte) val;
			start++;
		}
		
		String[] ips = ip.split("\\.");
		Logger.d(TAG, "Start parse ip string: " + ip + ", Sectons: " + ips.length);
		for (String section: ips) {
			response[start] = (byte) Integer.parseInt(section);
			start++;
		}
		
		byte[] result = new byte[start];
		System.arraycopy(response, 0, result, 0, start);
		Logger.d(TAG, "DNS Response package size: " + start);
		
		return result;
	}
}
