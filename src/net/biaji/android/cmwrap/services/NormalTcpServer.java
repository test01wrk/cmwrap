
package net.biaji.android.cmwrap.services;

import android.content.Context;
import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.utils.Utils;

public class NormalTcpServer implements WrapServer {

    private Context context;
    
    private ServerSocket serSocket;

    private final int servPort = 7443;

    private String name;

    private String proxyHost;

    private int proxyPort;

    private final String TAG = "TCPServer";

    private boolean inService = false;

    private ExecutorService serv = Executors.newCachedThreadPool();

    private static Hashtable<String, LinkRecord> connReq = new Hashtable<String, LinkRecord>();

    private final String[] iptablesRules = new String[] {
            "iptables -t nat -%3$s OUTPUT %1$s -p tcp -m multiport ! --destination-port 80,7442,7443,8000 -j DNAT  --to-destination 127.0.0.1:7443",
            " iptables -t nat -%3$s OUTPUT %1$s -p tcp -m multiport ! --destination-port 80,7442,7443,8000 -j LOG --log-uid --log-level info --log-prefix \"CMWRAP \""
    };

    public NormalTcpServer(String name) {
        this(null, name, "10.0.0.172", 80); //TODO: context should not null;
    }

    /**
     * @param name 服务名称
     * @param proxyHost HTTP代理服务器地址
     * @param proxyPort HTTP代理服务器端口
     */
    public NormalTcpServer(Context context, String name, String proxyHost, int proxyPort) {
        this.context = context;
        this.name = name;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        try {
            serSocket = new ServerSocket();
            serSocket.setReuseAddress(true);
            serSocket.bind(new InetSocketAddress(servPort));
            inService = true;
        } catch (IOException e) {
            Logger.e(TAG, "Server初始化错误，端口号" + servPort, e);
        }
        Logger.d(TAG, name + "启动于端口： " + servPort);

    }

    public boolean isClosed() {
        return (serSocket != null && serSocket.isClosed());
    }

    public void close() {
        inService = false;
        serv.shutdownNow();

        if (serSocket == null) {
            return;
        }

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
       
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;

    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;

    }

    public void run() {

        if (serSocket == null) {
            Logger.d(TAG, "Server socket NOT initilized yet.");
            return;
        }

        while (inService) {
            try {
                Logger.v(TAG, "等待客户端请求……");
                Socket socket = serSocket.accept();
                socket.setSoTimeout(120 * 1000);
                Logger.v(TAG, "获得客户端请求");

                String srcPort = socket.getPort() + "";
                Logger.d(TAG, "source port:" + srcPort);

                LinkRecord target = getTarget(srcPort);
                if (target == null) {
                    Logger.d(TAG, "SPT:" + srcPort + " doesn't match");
                    socket.close();
                    continue;
                } else {
                    PackageManager pm = context.getPackageManager();

                    String packageName = pm.getPackagesForUid(Integer.parseInt(target.uid))[0];
                    Logger.d(TAG, "package:" + packageName + " SPT:" + srcPort + "----->"
                            + target.destAddr + ":" + target.destPort);
                }
                serv.execute(new WapChannel(socket, target, proxyHost, proxyPort));

                TimeUnit.MILLISECONDS.sleep(100);
            } catch (IOException e) {
                Logger.e(TAG, "伺服客户请求失败" + e.getMessage());
            } catch (InterruptedException e) {
                Logger.e(TAG, "Interrupted:" + e.getMessage());
            } catch (RejectedExecutionException e) {
                Logger.e(TAG, "伺服客户请求失败" + e.getMessage());
            }
        }

        try {
            serSocket.close();
        } catch (IOException e) {
            Logger.e(TAG, name + "关闭服务端口失败：" + e.getMessage());
        }
        Logger.v(TAG, name + "侦听服务停止");

    }

    /**
     * 根据源端口号，由dmesg找出iptables记录的目的地址<br>
     * dmesg行示例：
     * 
     * <pre>
     * <6>[ 5739.482168] CMWRAP IN= OUT=ppp0 SRC=10.94.85.105 DST=121.14.125.26 LEN=60 TOS=0x00 PREC=0x00 TTL=64 ID=49871 DF PROTO=TCP SPT=51454 DPT=8080 WINDOW=13600 RES=0x00 SYN URGP=0 UID=10126 GID=10126
     * </pre>
     * 
     * @param sourcePort 连接源端口号
     * @return 连接描述
     */
    private synchronized LinkRecord getTarget(String sourcePort) {
        LinkRecord result = null;

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
            Process process = new ProcessBuilder().command("su").redirectErrorStream(true).start();

            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(command + Utils.LINEBREAK);
            os.flush();
            os.writeBytes("exit" + Utils.LINEBREAK);
            os.flush();

            out = process.getInputStream();
            BufferedReader outR = new BufferedReader(new InputStreamReader(out));
            String line = "";

            // 根据输出构建以源端口为key的地址表
            while ((line = outR.readLine()) != null) {

                boolean match = false;

                if (line.contains("CMWRAP")) {                  
                    
                    Logger.v(TAG, line);            
                    
                    Matcher m = Pattern.compile(".*DST=(.*?) .*SPT=(.*?) .*DPT=(.*?) .*UID=(.*?) .*").matcher(line);
                    if (m.find()) {
                        
                        LinkRecord record = new LinkRecord();
                        record.destAddr = m.group(1);
                        record.srcPort = m.group(2);
                        record.destPort = m.group(3);
                        record.uid = m.group(4);
                        
                        if(record.srcPort.equals(sourcePort)){
                            result = record;
                        } else {
                            connReq.put(record.srcPort ,record);
                            Logger.d(TAG, "connReq count:" + connReq.size());
                        }
                    }
                }
            }

            int execResult = process.waitFor();
            if (execResult == 0)
                Logger.v(TAG, command + " exec success");
            else {
                Logger.w(TAG, command + " exec with result " + execResult);
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

    public String[] getRules() {
        return iptablesRules;
    }

    /**
     * 描述单一连接的类
     * 
     * @author biaji
     */
    class LinkRecord {

        String uid;        

        String srcPort;

        String destPort;

        String destAddr;

    }

}
