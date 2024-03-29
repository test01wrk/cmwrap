
package net.biaji.android.cmwrap.services;

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

import net.biaji.android.cmwrap.Logger;
import net.biaji.android.cmwrap.utils.Utils;

public class NormalTcpServer implements WrapServer {

    private ServerSocket serSocket;

    private final int servPort = 7443;

    private String target;

    private String name;

    private String proxyHost;

    private int proxyPort;

    private final String TAG = "TCPServer";

    private boolean inService = false;

    private ExecutorService serv = Executors.newCachedThreadPool();

    private static Hashtable<String, String> connReq = new Hashtable<String, String>();

    private final String[] iptablesRules = new String[] {
            "iptables -t nat -%3$s OUTPUT %1$s -p tcp -m multiport ! --destination-port 80,7442,7443,8000 -j DNAT  --to-destination 127.0.0.1:7443",
            " iptables -t nat -%3$s OUTPUT %1$s -p tcp -m multiport ! --destination-port 80,7442,7443,8000 -j LOG --log-level info --log-prefix \"CMWRAP \""
    };

    public NormalTcpServer(String name) {
        this(name, "10.0.0.172", 80);
    }

    /**
     * @param name
     *            服务名称
     * @param proxyHost
     *            HTTP代理服务器地址
     * @param proxyPort
     *            HTTP代理服务器端口
     */
    public NormalTcpServer(String name, String proxyHost, int proxyPort) {
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
        this.target = dest;
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

                target = getTarget(srcPort);
                if (target == null || target.trim().equals("")) {
                    Logger.d(TAG, "SPT:" + srcPort + " doesn't match");
                    socket.close();
                    continue;
                } else {
                    Logger.d(TAG, "SPT:" + srcPort + "----->" + target);
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
                    String addr = "", destPort = "", srcPort = "";
                    String[] parmArr = line.split(" ");
                    for (String parm : parmArr) {
                        String trimParm = parm.trim();
                        if (trimParm.startsWith("DST")) {
                            addr = getValue(trimParm);
                        }

                        if (trimParm.startsWith("SPT")) {
                            if (sourcePort.equals(getValue(trimParm))) {
                                match = true;
                            } else {
                                srcPort = getValue(trimParm);
                            }
                        }

                        if (trimParm.startsWith("DPT")) {
                            destPort = getValue(trimParm);
                        }

                    }

                    if (match)
                        result = addr + ":" + destPort;
                    else
                        connReq.put(srcPort, addr + ":" + destPort);

                }
            }

            int execResult = process.waitFor();
            if (execResult == 0)
                Logger.d(TAG, command + " exec success");
            else {
                Logger.d(TAG, command + " exec with result " + execResult);
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

}
