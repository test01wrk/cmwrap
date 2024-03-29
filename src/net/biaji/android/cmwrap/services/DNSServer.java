
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

import net.biaji.android.cmwrap.Config;
import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.utils.Utils;

/**
 * 此类实现了DNS代理
 * 
 * @author biaji
 */
public class DNSServer implements WrapServer {

    private final String TAG = "DNSServer";

    private String homePath;

    private final String CACHE_PATH = "/cache";

    private final String CACHE_FILE = "/dnscache";

    private DatagramSocket srvSocket;

    private int srvPort = 7442;

    private String name;

    protected String proxyHost, dnsHost;

    protected int proxyPort, dnsPort;

    final protected int DNS_PKG_HEADER_LEN = 12;

    final private int[] DNS_HEADERS = {
            0, 0, 0x81, 0x80, 0, 0, 0, 0, 0, 0, 0, 0
    };

    final private int[] DNS_PAYLOAD = {
            0xc0, 0x0c, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x3c, 0x00, 0x04
    };

    final private int IP_SECTION_LEN = 4;

    private boolean inService = false;

    private Hashtable<String, DnsResponse> dnsCache = new Hashtable<String, DnsResponse>();

    /**
     * 内建自定义缓存
     */
    private Hashtable<String, String> orgCache = new Hashtable<String, String>();

    private String target = "8.8.4.4:53";

    private final String[] iptablesRules = new String[] {
        "iptables -t nat -%3$s OUTPUT %1$s -p udp  --dport 53  -j DNAT  --to-destination 127.0.0.1:7442"
    };

    public DNSServer(String name, int port, String proxyHost, int proxyPort, String dnsHost,
            int dnsPort) {
        this.name = name;
        this.srvPort = port;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.dnsHost = dnsHost;
        this.dnsPort = dnsPort;

        if (dnsHost != null && !dnsHost.equals(""))
            target = dnsHost + ":" + dnsPort;

        Utils.flushDns(Config.DEFAULT_DNS_ADD);
        initOrgCache();

        try {
            srvSocket = new DatagramSocket(srvPort, InetAddress.getByName("127.0.0.1"));
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
                DatagramPacket dnsq = new DatagramPacket(qbuffer, qbuffer.length);

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

                    sendDns(dnsCache.get(questDomain).getDnsResponse(), dnsq, srvSocket);

                    Logger.d(TAG, "命中缓存");

                } else if (orgCache.containsKey(questDomain)) { // 如果为自定义域名解析
                    byte[] ips = parseIPString(orgCache.get(questDomain));
                    byte[] answer = createDNSResponse(udpreq, ips);
                    addToCache(questDomain, answer);
                    sendDns(answer, dnsq, srvSocket);
                    Logger.d(TAG, "自定义解析" + orgCache);
                } else {
                    starTime = System.currentTimeMillis();
                    byte[] answer = fetchAnswer(udpreq);
                    if (answer != null && answer.length != 0) {
                        addToCache(questDomain, answer);
                        sendDns(answer, dnsq, srvSocket);
                        Logger.d(
                                TAG,
                                "正确返回DNS解析，长度：" + answer.length + "  耗时："
                                        + (System.currentTimeMillis() - starTime) / 1000 + "s");
                    } else {
                        Logger.e(TAG, "返回DNS包长为0");
                    }

                }

                /* For test, validate dnsCache */
                /*
                 * if (dnsCache.size() > 0) { Logger.d(TAG,
                 * "Domains in cache:"); Enumeration<String> enu =
                 * dnsCache.keys(); while (enu.hasMoreElements()) { String
                 * domain = (String) enu.nextElement(); DnsResponse resp =
                 * dnsCache.get(domain); Logger.d(TAG, domain + " : " +
                 * resp.getIPString()); } }
                 */

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
     * @param quest 原始DNS请求
     * @return
     */
    protected byte[] fetchAnswer(byte[] quest) {

        Socket innerSocket = new InnerSocketBuilder(proxyHost, proxyPort, target).getSocket();
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
     * @param response 应答包
     * @param dnsq 请求包
     * @param srvSocket 侦听Socket
     */
    private void sendDns(byte[] response, DatagramPacket dnsq, DatagramSocket srvSocket) {

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
     * @param request dns udp包
     * @return 请求的域名
     */
    protected String getRequestDomain(byte[] request) {
        String requestDomain = "";
        int reqLength = request.length;
        if (reqLength > 13) { // 包含包体
            byte[] question = new byte[reqLength - 12];
            System.arraycopy(request, 12, question, 0, reqLength - 12);
            requestDomain = parseDomain(question);
            requestDomain = requestDomain.substring(0, requestDomain.length() - 1);
        }
        return requestDomain;
    }

    /*
     * Create a DNS response packet, which will send back to application.
     * @author yanghong Reference to: Mini Fake DNS server (Python)
     * http://code.activestate.com/recipes/491264-mini-fake-dns-server/ DOMAIN
     * NAMES - IMPLEMENTATION AND SPECIFICATION
     * http://www.ietf.org/rfc/rfc1035.txt
     */
    protected byte[] createDNSResponse(byte[] quest, byte[] ips) {
        byte[] response = null;
        int start = 0;

        response = new byte[128];

        for (int val : DNS_HEADERS) {
            response[start] = (byte) val;
            start++;
        }

        System.arraycopy(quest, 0, response, 0, 2); /* 0:2 */
        System.arraycopy(quest, 4, response, 4, 2); /* 4:6 -> 4:6 */
        System.arraycopy(quest, 4, response, 6, 2); /* 4:6 -> 7:9 */
        System.arraycopy(quest, DNS_PKG_HEADER_LEN, response, start, quest.length
                - DNS_PKG_HEADER_LEN); /* 12:~ -> 15:~ */
        start += quest.length - DNS_PKG_HEADER_LEN;

        for (int val : DNS_PAYLOAD) {
            response[start] = (byte) val;
            start++;
        }

        /* IP address in response */
        for (byte ip : ips) {
            response[start] = ip;
            start++;
        }

        byte[] result = new byte[start];
        System.arraycopy(response, 0, result, 0, start);
        Logger.d(TAG, "DNS Response package size: " + start);

        return result;
    }

    /*
     * Parse IP string into byte, do validation.
     * @param ip IP string
     * @return IP in byte array
     */
    protected byte[] parseIPString(String ip) {
        byte[] result = null;
        int value;
        int i = 0;
        String[] ips = null;

        ips = ip.split("\\.");

        Logger.d(TAG, "Start parse ip string: " + ip + ", Sectons: " + ips.length);

        if (ips.length != IP_SECTION_LEN) {
            Logger.e(TAG, "Malformed IP string number of sections is: " + ips.length);
            return null;
        }

        result = new byte[IP_SECTION_LEN];

        for (String section : ips) {
            try {
                value = Integer.parseInt(section);

                /* 0.*.*.* and *.*.*.0 is invalid */
                if ((i == 0 || i == 3) && value == 0) {
                    return null;
                }

                result[i] = (byte) value;
                i++;
            } catch (NumberFormatException e) {
                Logger.e(TAG, "Malformed IP string section: " + section);
                return null;
            }
        }

        return result;
    }

    /**
     * 解析域名
     * 
     * @param request
     * @return
     */
    private String parseDomain(byte[] request) {

        String result = "";
        int length = request.length;
        int partLength = request[0];
        if (partLength == 0)
            return result;
        try {
            byte[] left = new byte[length - partLength - 1];
            System.arraycopy(request, partLength + 1, left, 0, length - partLength - 1);
            result = new String(request, 1, partLength) + ".";
            result += parseDomain(left);
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

            Hashtable<String, DnsResponse> tmpCache = (Hashtable<String, DnsResponse>) dnsCache
                    .clone();
            for (DnsResponse resp : dnsCache.values()) {
                // 检查缓存时效(十天)
                if ((System.currentTimeMillis() - resp.getTimestamp()) > 864000000L) {
                    Logger.d(TAG, "删除" + resp.getRequest() + "记录");
                    tmpCache.remove(resp.getRequest());
                }
            }

            dnsCache = tmpCache;
            tmpCache = null;

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

    /**
     * 在缓存中添加一个域名解析
     * 
     * @param questDomainName 域名
     * @param answer 解析结果
     */
    private void addToCache(String questDomainName, byte[] answer) {
        DnsResponse response = new DnsResponse(questDomainName);
        response.setDnsResponse(answer);
        dnsCache.put(questDomainName, response);
        saveCache();
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

    public String[] getRules() {
        return iptablesRules;
    }

    private void initOrgCache() {
        // TODO: 由Preference读取
        // TODO: 重构
        orgCache.put("dn5r3l4y.appspot.com", "74.125.153.141");

    }

    public boolean test(String domain, String ip) {
        boolean ret = true;

        // TODO: Implement test case

        return ret;
    }

}

/**
 * 此类封装了一个Dns回应
 * 
 * @author biaji
 */
class DnsResponse implements Serializable {

    private static final long serialVersionUID = -6693216674221293274L;

    private String request = null;

    private long timestamp = System.currentTimeMillis();;

    private int reqTimes = 0;

    private byte[] dnsResponse = null;

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
        return dnsResponse;
    }

    /**
     * @param dnsResponse the dnsResponse to set
     */
    public void setDnsResponse(byte[] dnsResponse) {
        this.dnsResponse = dnsResponse;
    }

    /**
     * @return IP string
     */
    public String getIPString() {
        String ip = null;
        int i;

        if (dnsResponse == null) {
            return null;
        }

        i = dnsResponse.length - 4;

        if (i < 0) {
            return null;
        }

        ip = "" + (int) (dnsResponse[i] & 0xFF); /* Unsigned byte to int */

        for (i++; i < dnsResponse.length; i++) {
            ip += "." + (int) (dnsResponse[i] & 0xFF);
        }

        return ip;
    }
}
