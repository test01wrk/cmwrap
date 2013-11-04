
package net.biaji.android.cmwrap;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.TreeSet;

import net.biaji.android.cmwrap.utils.Utils;
import android.util.Log;

/**
 * 用于管理iptables规则和状态的类
 * 
 * @author biAji<biaji@biaji.net>
 */
public class IptablesManager {

    private static TreeSet<String> rules = new TreeSet<String>();;

    private static IptablesManager instance = new IptablesManager();

    private final String TAG = "IptablesManager";

    private final String IP_FORWARD_ENABLE = "echo 1 > /proc/sys/net/ipv4/ip_forward";

    /**
     * 添加命令
     */
    private final String IPTABLES_ADD = "A";

    /**
     * 删除命令
     */
    private final String IPTABLES_DEL = "D";

    private final String IPTABLES_RULE_HTTP = "iptables -t nat -%3$s OUTPUT %1$s -p tcp  --dport 80  -j DNAT  --to-destination %2$s";

    private boolean onlyMobile = true;

    private String proxyHost = "10.0.0.172";

    private int proxyPort = 80;

    private IptablesManager() {
        rules.add(IPTABLES_RULE_HTTP);
    }

    /**
     * 获取iptablesManager的实例
     * 
     * @return iptablesManager的实例
     */
    public static IptablesManager getInstance() {
        return instance;
    }

    /**
     * 启用iptables规则
     */
    public void enable() {

        Utils.rootCMD(IP_FORWARD_ENABLE);
        execute(IPTABLES_ADD);
    }

    /**
     * 禁用iptables
     */
    public void disable() {
        execute(IPTABLES_DEL);
    }

    /**
     * 具体执行脚本操作
     * 
     * @param cmd 添加/删除，参见{@link#IPTABLES_ADD}，{@link#IPTABLES_DEL}
     */
    private void execute(String cmd) {
        String inface = " ";

        if (rules.isEmpty()) {
            Logger.d(TAG, "Iptables: No rule to delete");
            return;
        }

        if (onlyMobile) {
            inface = " -o " + getInterfaceName();
        }

        for (String rule : rules) {
            try {
                rule = String.format(rule, inface, this.proxyHost + ":" + this.proxyPort, cmd);
                Utils.rootCMD(rule);
            } catch (Exception e) {
                Logger.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    /**
     * 添加iptables规则
     * 
     * @param rule iptables规则
     */
    public void addRule(String rule) {
        if (rules.isEmpty())
            rules.add(IPTABLES_RULE_HTTP);
        if (!rules.contains(rule))
            rules.add(rule);
    }

    /**
     * 添加所有规则至现有规则列表
     * 
     * @param newRules 新规则列表
     */
    public void addAllRules(Collection<? extends String> newRules) {
        if (rules.isEmpty())
            rules.add(IPTABLES_RULE_HTTP);
        rules.addAll(newRules);
    }

    public void removeRule(String rule) {
        rules.remove(rule);
    }

    public boolean isOnlyMobile() {
        return onlyMobile;
    }

    public void setOnlyMobile(boolean onlyMobile) {
        this.onlyMobile = onlyMobile;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    /**
     * 获取网络名称
     * 
     * @return 当前移动网络界面名称
     */
    private String getInterfaceName() {
        String result = "rmnet0";

        try {
            for (Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces(); interfaces
                    .hasMoreElements();) {

                NetworkInterface nInterface = interfaces.nextElement();

                if (nInterface.isLoopback()) {
                    continue;
                }

                if (!nInterface.isUp()) {
                    continue;
                }

                String interfacename = nInterface.getName();

                if (interfacename.contains("usb") || interfacename.contains("wifi")
                        || interfacename.contains("wlan") || interfacename.contains("rndis")) {
                    continue;
                }

                if (!nInterface.getInetAddresses().hasMoreElements()) {
                    continue;
                }

                return interfacename;

            }
        } catch (SocketException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return result;
    }

}
