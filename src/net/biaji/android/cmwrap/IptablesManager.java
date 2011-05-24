
package net.biaji.android.cmwrap;

import net.biaji.android.cmwrap.utils.Utils;

import android.util.Log;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;

/**
 * 用于管理iptables规则和状态的类
 * 
 * @author biAji<biaji@biaji.net>
 */
public class IptablesManager {

    private static HashSet<String> rules = new HashSet<String>();;

    private static IptablesManager instance = new IptablesManager();

    private final String TAG = "IptablesManager";

    private final String IP_FORWARD_ENABLE = "echo 1 > /proc/sys/net/ipv4/ip_forward";

    private final String IPTABLES_CMD_DISABLE = "iptables -t nat -F";

    private final String IPTABLES_RULE_HTTP = "iptables -t nat -A OUTPUT %1$s -p tcp  --dport 80  -j DNAT  --to-destination %2$s";

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

        String inface = " ";

        if (rules.isEmpty()) {
            Logger.d(TAG, "Iptables: No rule to apply");
            return;
        }

        if (onlyMobile) {
            inface = " -o " + getInterfaceName();
        }

        for (String rule : rules) {
            try {
                rule = String.format(rule, inface, this.proxyHost + ":" + this.proxyPort);
                Utils.rootCMD(rule);

            } catch (Exception e) {
                Logger.e(TAG, e.getLocalizedMessage());
            }
        }
    }

    /**
     * 禁用iptables
     */
    public void disable() {
        Utils.rootCMD(IPTABLES_CMD_DISABLE);
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

    /**
     * 清理所有iptables规则
     */
    public void clear() {
        rules.clear();
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
                String interfacename = interfaces.nextElement().getName();
                if (!interfacename.contains("lo") && !interfacename.contains("usb")
                        && !interfacename.contains("wifi") && !interfacename.contains("wlan")) {
                    // 如果不是lo，也不是usb，也不是wifi，就假定是移动网络 >.<
                    return interfacename;
                }
            }
        } catch (SocketException e) {
            Log.e(TAG, e.getLocalizedMessage());
        }
        return result;
    }

}
