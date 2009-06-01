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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;

import net.biaji.android.cmwrap.services.WrapService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Cmwrap extends Activity implements OnClickListener {

	private static boolean inService = false;

	private String proxyHost;

	private int proxyPort;

	private ArrayList<Rule> rules = new ArrayList<Rule>();

	private TextView logWindow;

	final String TAG = "CMWRAP->";

	private final int VER = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		SharedPreferences pref = getSharedPreferences("cmwrap", MODE_PRIVATE);
		inService = pref.getBoolean("STATUS", false);

		proxyHost = getResources().getString(R.string.proxyServer);
		proxyPort = Integer.parseInt(getResources().getString(
				R.string.proxyPort));

		rules = Utils.loadRules(this);

		logWindow = (TextView) findViewById(R.id.logwindow);

		Button switcher = (Button) findViewById(R.id.Switch);
		setButton();

		switcher.setOnClickListener(this);

		if (Utils.isCmwap(this))
			logWindow.append("当前数据连接为cmwap\n");
		else {
			logWindow.append("当前数据连接不是cmwap\n");
			switcher.setEnabled(false);
		}

		if (hasHosts()) {
			logWindow.append("hosts文件不须更新\n");
		} else {
			logWindow.append("hosts文件更新...\n");
			int result = Utils.rootCMD(getString(R.string.CMDremount));
			if (result != 0) {
				logWindow.append(getString(R.string.ERR_NO_ROOT));
				switcher.setEnabled(false);
			} else {
				installFiles("/system/etc/hosts", R.raw.hosts, null);
				logWindow.append("更新完毕。\n");
			}
		}

		// appStatus();

	}

	@Override
	protected void onPause() {
		super.onPause();
		SharedPreferences pref = getSharedPreferences("cmwrap", MODE_PRIVATE);
		SharedPreferences.Editor editor = pref.edit();
		editor.putBoolean("STATUS", inService);
		editor.commit();
	}

	public void onClick(View v) {
		logWindow = (TextView) findViewById(R.id.logwindow);

		Intent serviceIn = new Intent(this, WrapService.class);

		if (inService) {
			stopService(serviceIn);
			Log.i(TAG, "禁用iptables转向...");
			Utils.rootCMD(getString(R.string.CMDiptablesDisable));
			Toast.makeText(this, R.string.serviceTagDown, Toast.LENGTH_SHORT)
					.show();
			inService = false;
			setButton();
		} else {
			startService(serviceIn);
			Log.i(TAG, "启用iptables转向...");
			Utils.rootCMD(getString(R.string.CMDipForwardEnable));
			Utils.rootCMD(getString(R.string.CMDiptablesDisable));

			forward();

			Toast.makeText(this, R.string.serviceTagUp, Toast.LENGTH_SHORT)
					.show();
			inService = true;
			setButton();
		}

	}

	private void setButton() {
		Button switcher = (Button) findViewById(R.id.Switch);
		if (inService) {
			switcher.setText(R.string.buttonDisable);
		} else {
			switcher.setText(R.string.buttonEnable);
		}
	}

	/**
	 * 判断程序安装状态
	 * 
	 * @return -1 初次安装 0 升级安装 1 同版本再次安装
	 */
	private int appStatus() {
		int firsTime = -1;
		// 判断状态
		try {
			FileInputStream in = openFileInput("Version");
			int ver = in.read();
			if (ver != 0 && ver < VER) {
				firsTime = 1;
			} else
				firsTime = 0;
			in.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "No Version File, First installed");
		} catch (IOException e) {
			Log.e(TAG, "IOerror");
		}
		return firsTime;
	}

	/**
	 * 判断hosts文件是否更新
	 * 
	 * @return
	 */
	private boolean hasHosts() {
		boolean result = false;
		File hosts = new File("/system/etc/hosts");
		if (hosts.length() > 2000 && hosts.length() < 5000)
			result = true;
		return result;
	}

	private void forward() {

		try {
			for (Rule rule : rules) {
				String cmd;
				if (rule.mode == Rule.MODE_BASE)
					cmd = "iptables -t nat -A OUTPUT -o rmnet0 -p tcp --dport "
							+ rule.desPort + " -j DNAT --to-destination "
							+ proxyHost + ":" + proxyPort;
				else
					cmd = "iptables -t nat -A OUTPUT -o rmnet0 -p tcp -d "
							+ rule.desHost + " --dport " + rule.desPort
							+ " -j DNAT --to-destination 127.0.0.1:"
							+ rule.servPort;
				Utils.rootCMD(cmd);

			}

		} catch (Exception e) {
		}

	}

	/**
	 * 安装文件
	 * 
	 * @param dest
	 *            安装路径
	 * @param resId
	 *            资源文件id
	 * @param mod
	 *            文件属性
	 * @return
	 */
	public int installFiles(String dest, int resId, String mod) {
		int result = -1;
		BufferedInputStream bin = null;
		FileOutputStream fo = null;
		try {
			bin = new BufferedInputStream(getResources().openRawResource(resId));

			if (mod == null)
				mod = "644";
			Utils.rootCMD("chmod 666 " + dest);
			File destF = new File(dest);

			fo = new FileOutputStream(destF);
			int length;
			byte[] content = new byte[1024];

			while ((length = bin.read(content)) > 0) {
				fo.write(content, 0, length);
			}

			fo.close();
			bin.close();
			Utils.rootCMD("chmod " + mod + "  " + dest);
			result = 0;

		} catch (FileNotFoundException e) {
			Log.e(TAG, "未发现目的路径", e);
		} catch (IOException e) {
			Log.e(TAG, "安装文件错误", e);
		}
		return result;
	}
}