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
import java.util.ArrayList;

import net.biaji.android.cmwrap.services.WrapService;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Cmwrap extends Activity implements OnClickListener {

	private static boolean inService = false;

	private String proxyHost;

	private int proxyPort;

	private ArrayList<Rule> rules = new ArrayList<Rule>();

	private TextView logWindow;

	private final String TAG = "CMWRAP->";

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

		ToggleButton switcher = (ToggleButton) findViewById(R.id.Switch);
		switcher.setOnClickListener(this);

		ToggleButton baseServiceSwitcher = (ToggleButton) findViewById(R.id.BaseService);
		baseServiceSwitcher.setOnClickListener(this);
		baseServiceSwitcher.setEnabled(false); // TODO

		//判断是否需要更新hosts文件
		if (appStatus() == 1) {
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
		
		serviceIn.putExtra("SERVERLEVEL", WrapService.SERVER_LEVEL_APPS);  //TODO 详分类别

		ToggleButton switcher = (ToggleButton) findViewById(R.id.Switch);

		if (inService) {
			stopService(serviceIn);
			Log.i(TAG, "禁用服务");
			Utils.rootCMD(getString(R.string.CMDiptablesDisable));
			Toast.makeText(this, R.string.serviceTagDown, Toast.LENGTH_SHORT)
					.show();
			inService = false;
			switcher.setChecked(inService);
		} else {
			Log.i(TAG, "启用服务");
			startService(serviceIn);
			Toast.makeText(this, R.string.serviceTagUp, Toast.LENGTH_SHORT)
					.show();
			inService = true;
			switcher.setChecked(inService);
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		boolean result = super.onCreateOptionsMenu(menu);

		menu.add(R.string.MENU_TEST);

		return result;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
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

			if (ver == Integer.parseInt(getString(R.string.Version))) {
				firsTime = 1;
			} else {
				firsTime = 0;
				tag();
			}
			in.close();
		} catch (FileNotFoundException e) {
			Log.e(TAG, "No Version File, First installed");
			tag();
		} catch (IOException e) {
			Log.e(TAG, "IOerror");
		}
		return firsTime;
	}

	/**
	 * 更新版本号
	 */
	private void tag() {
		FileOutputStream out;
		try {
			out = openFileOutput("Version", MODE_PRIVATE);

			out.write(Integer.parseInt(getString(R.string.Version)));
			out.close();
		} catch (FileNotFoundException ex) {
			Log.e(TAG, "No Version File, First installed");
		} catch (NumberFormatException ex) {
			Log.e(TAG, "版本号解析失败");
		} catch (IOException ex) {
			Log.e(TAG, "写入版本号失败");
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