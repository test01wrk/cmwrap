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
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Properties;

import net.biaji.android.cmwrap.services.WrapService;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class Cmwrap extends Activity implements OnClickListener {

	private static boolean inService = false;

	private TextView logWindow;

	private final String TAG = "CMWRAP->";

	private final int VER = 1;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		logWindow = (TextView) findViewById(R.id.logwindow);

		Button switcher = (Button) findViewById(R.id.Switch);
		setButton();

		switcher.setOnClickListener(this);

		if (isCmwap())
			logWindow.append("cmwap detected\n");

		if (hasHosts()) {
			logWindow.append("hosts文件不须更新\n");
		} else {
			logWindow.append("hosts文件更新...\n");
			int result = rootCMD(getString(R.string.CMDremount));
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

	public void onClick(View v) {
		logWindow = (TextView) findViewById(R.id.logwindow);

		Intent serviceIn = new Intent(this, WrapService.class);

		if (inService) {
			// stopService(serviceIn);
			Log.i(TAG, "禁用iptables转向...");
			rootCMD(getString(R.string.CMDiptablesDisable));
			Toast.makeText(this, R.string.serviceTagDown, Toast.LENGTH_SHORT)
					.show();
			inService = false;
			setButton();
		} else {
			// startService(serviceIn);
			Log.i(TAG, "启用iptables转向...");
			rootCMD(getString(R.string.CMDipForwardEnable));
			rootCMD(getString(R.string.CMDiptablesDisable));
			String cmd = getString(R.string.CMDiptablesEnable);
			for (String subCmd : cmd.split("｜"))
				rootCMD(subCmd.trim());
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

	private boolean isCmwap() {
		boolean result = false;
		try {
			NetworkInterface nf = NetworkInterface.getByName("rmnet0");
			if (nf != null
					&& nf.getInetAddresses().nextElement().getHostAddress()
							.toString().startsWith("10.")) {
				result = true;
			}
		} catch (SocketException e) {
			Log.e(TAG, "Can not get Network info");
		}

		return result;
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
		if (hosts.length() > 200)
			result = true;
		return result;
	}

	/**
	 * 以root权限执行命令
	 * 
	 * @param 需要执行的指令
	 * @return -1 执行失败； 0 执行正常
	 */
	private int rootCMD(String cmd) {
		int result = -1;
		DataOutputStream os = null;
		InputStream err = null, out = null;
		try {
			Process process = Runtime.getRuntime().exec("su");
			err = process.getErrorStream();
			BufferedReader bre = new BufferedReader(new InputStreamReader(err),
					1024 * 8);

			out = process.getInputStream();

			os = new DataOutputStream(process.getOutputStream());

			os.writeBytes(cmd + "\n");
			os.flush();
			os.writeBytes("exit\n");
			os.flush();

			String resp;
			while ((resp = bre.readLine()) != null) {
				Log.d(TAG, resp);
			}
			result = process.waitFor();
			if (result == 0)
				Log.d(TAG, cmd + " exec success");
			else {
				Log.d(TAG, cmd + " exec with result" + result);
			}
			os.close();
			process.destroy();
		} catch (IOException e) {
			Log.e(TAG, "Failed to exec command", e);
		} catch (InterruptedException e) {
			Log.e(TAG, "线程意外终止", e);
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
	private int installFiles(String dest, int resId, String mod) {
		int result = -1;
		BufferedInputStream bin = null;
		FileOutputStream fo = null;
		try {
			bin = new BufferedInputStream(getResources().openRawResource(resId));

			if (mod == null)
				mod = "644";
			rootCMD("chmod 666 " + dest);
			File destF = new File(dest);

			fo = new FileOutputStream(destF);
			int length;
			byte[] content = new byte[1024];

			while ((length = bin.read(content)) > 0) {
				fo.write(content, 0, length);
			}

			fo.close();
			bin.close();
			rootCMD("chmod " + mod + "  "+ dest);
			result = 0;

		} catch (FileNotFoundException e) {
			Log.e(TAG, "未发现目的路径", e);
		} catch (IOException e) {
			Log.e(TAG, "安装文件错误", e);
		}
		return result;
	}
}