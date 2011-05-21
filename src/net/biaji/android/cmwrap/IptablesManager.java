
package net.biaji.android.cmwrap;

import java.util.ArrayList;

/**
 * 用于管理iptables规则和状态的类
 * 
 * @author biAji<biaji@biaji.net>
 */
public class IptablesManager {

    private static IptablesManager instance = new IptablesManager();

    private static boolean inited = false;

    private static ArrayList<String> rules = new ArrayList<String>();

    private IptablesManager() {
    }

    public IptablesManager getInstance() {
        return instance;
    }

    /**
     * 清理所有iptables规则 TODO:细化粒度
     */
    public void clean() {

    }

    /**
     * 添加iptables规则
     * 
     * @param rule
     */
    public void addRule(String rule) {

    }

    public void removeRule(String rule) {

    }

}
