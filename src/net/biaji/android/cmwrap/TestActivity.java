/**
 *  This program is free software; you can redistribute it and/or modify it under 
 *  the terms of the GNU General Public License as published by the Free Software 
 *  Foundation; either version 3 of the License, or (at your option) any later 
 *  version.
 *  You should have received a copy of the GNU General Public License along with 
 *  this program; if not, see <http://www.gnu.org/licenses/>. 
 *  Use this application at your own risk.
 *  
 *  此程序为开源软件，遵循GPLv3版本发布，并受其保护。
 *
 *  Copyright (c) 2009 by biAji
 */

package net.biaji.android.cmwrap;

import net.biaji.android.cmwrap.services.WapChannel;
import net.biaji.android.cmwrap.utils.Utils;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.concurrent.TimeUnit;

/**
 * 用于环境测试的Activity
 * 
 * @author biAji<biaji@biaji.net>
 */
public final class TestActivity extends Activity {

    private final String TAG = "CMWRAP->Test";

    private ProgressBar progressBar = null;

    private TextView logWindow = null, progressDescribe = null;

    private String proxyHost, DNSServer;

    private int proxyPort;

    private Context context;

    private Handler handler = new Handler() {
        int progress = 0;

        @Override
        public void handleMessage(Message msg) {
            progress = msg.getData().getInt("PROGRESS");
            if (progress >= 100) {
                progressBar.setProgress(0);
            } else {
                progressBar.setProgress(progress);
            }
            String testName = msg.getData().getString("TESTNAME");
            String stepMsg = msg.getData().getString("MESSAGE");
            String errMsg = msg.getData().getString("ERRMSG");
            Logger.d(TAG, "testName: " + testName);
            progressDescribe.setText(stepMsg);
            if (errMsg != null) {
                logWindow.append(errMsg);
            } else {
                logWindow.append(testName + getString(R.string.TEST_PASSED));
            }
        }
    };;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test);
        proxyHost = Config.getStringPref(this, "PROXYHOST", "10.0.0.172");
        proxyPort = Integer.parseInt(Config.getStringPref(this, "PROXYPORT", "80"));
        progressBar = (ProgressBar) findViewById(R.id.TestProgress);
        logWindow = (TextView) findViewById(R.id.logOut);
        progressDescribe = (TextView) findViewById(R.id.progressDescribe);
        context = this;
        new TestManager(handler).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private class TestManager extends Thread {

        Handler handler = null;

        TestManager(Handler handler) {
            this.handler = handler;
        }

        public void run() {
            Message msg = handler.obtainMessage();
            Bundle bundle = new Bundle();

            int result = Utils.rootCMD(getString(R.string.iptables_test_str));
            bundle.putString("TESTNAME", getString(R.string.TEST_ROOT));
            if (result == 1 || result == -1) { // 没有root权限
                bundle.putString("ERRMSG", getString(R.string.ERR_NO_ROOT));
                bundle.putInt("PROGRESS", 100);
                msg.setData(bundle);
                handler.sendMessage(msg);
                return;
            }
            bundle.putInt("PROGRESS", 10);
            bundle.putString("MESSAGE", getString(R.string.TEST_IPTABLES));
            msg.setData(bundle);
            handler.sendMessage(msg);
            testSleep(1000);

            // 测试iptables是否存在
            msg = handler.obtainMessage();
            bundle.putString("TESTNAME", getString(R.string.TEST_IPTABLES));
            if (result == 127 || result == 126) { // 没有iptables， 或权限不对
                bundle.putString("ERRMSG", getString(R.string.ERR_NO_IPTABLES));
                bundle.putInt("PROGRESS", 100);
                msg.setData(bundle);
                handler.sendMessage(msg);
                return;
            }
            bundle.putInt("PROGRESS", 20);
            bundle.putString("MESSAGE", getString(R.string.TEST_CMWAP));
            msg.setData(bundle);
            handler.sendMessage(msg);
            testSleep(1000);

            // 测试连接方式是否为cmwap
            msg = handler.obtainMessage();
            bundle.putString("TESTNAME", getString(R.string.TEST_CMWAP));
            if (!Utils.isCmwap(context)) {
                bundle.putString("ERRMSG", getString(R.string.ERR_NOT_CMWAP));
                bundle.putInt("PROGRESS", 100);
            }
            bundle.putInt("PROGRESS", 40);
            bundle.putString("MESSAGE", getString(R.string.TEST_HTTPS));
            msg.setData(bundle);
            handler.sendMessage(msg);
            testSleep(1000);

            // 测试https
            msg = handler.obtainMessage();
            bundle.putString("TESTNAME", getString(R.string.TEST_HTTPS));
            WapChannel channel = new WapChannel(null, proxyHost, proxyPort);
            testSleep(5000);
            if (!channel.isConnected()) {
                bundle.putString("ERRMSG", getString(R.string.ERR_UNSUPPORT_HTTPS));
            }
            channel.destory();
            bundle.putInt("PROGRESS", 60);
            bundle.putString("MESSAGE", getString(R.string.TEST_OTHER));
            msg.setData(bundle);
            handler.sendMessage(msg);

            // 测试DNS
            boolean httpDnsEnabled = Config.getBooleanPref(context, "HTTPDNSENABLED", true);
            // 启用http dns之后暂时不进行此项检测
            // TODO 检测http DNS可用性
            if (!httpDnsEnabled) {
                DNSServer = Config.getStringPref(context, "DNSADD", "");
                msg = handler.obtainMessage();
                bundle.putString("TESTNAME", getString(R.string.TEST_DNS));
                channel = new WapChannel(null, DNSServer + ":53", proxyHost, proxyPort);
                testSleep(5000);
                if (!channel.isConnected()) {
                    bundle.putString("ERRMSG", getString(R.string.ERR_UNSUPPORT_DNS));
                }
                channel.destory();
                bundle.putInt("PROGRESS", 80);
                bundle.putString("MESSAGE", getString(R.string.TEST_OTHER));
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            // 测试Gtalk
            msg = handler.obtainMessage();
            bundle.putString("TESTNAME", getString(R.string.TEST_OTHER));
            channel = new WapChannel(null, "mtalk.google.com:5228", proxyHost, proxyPort);
            testSleep(5000);
            if (!channel.isConnected()) {
                bundle.putString("ERRMSG", getString(R.string.ERR_UNSUPPORT_OTHERS));
            }
            channel.destory();
            bundle.putInt("PROGRESS", 100);
            msg.setData(bundle);
            handler.sendMessage(msg);

        }

        private void testSleep(long time) {
            try {
                TimeUnit.MILLISECONDS.sleep(time);
            } catch (InterruptedException e) {
            }
        }
    }
}
