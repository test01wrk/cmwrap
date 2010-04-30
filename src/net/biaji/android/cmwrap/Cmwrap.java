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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import net.biaji.android.cmwrap.services.WapChannel;
import net.biaji.android.cmwrap.services.WrapService;
import net.biaji.android.cmwrap.utils.Utils;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class Cmwrap extends Activity implements OnClickListener {

	private TextView logWindow;

	private int serviceLevel = WrapService.SERVER_LEVEL_NULL;

	private String proxyHost, DNSServer;

	private int proxyPort;

	private final String TAG = "CMWRAP->";

	private final int DIALOG_ABOUT_ID = 0;

	private final int DIALOG_TEST_ID = 1;

	private final int APP_STATUS_NEW = -1;

	private final int APP_STATUS_UPDATE = 0;

	private final int APP_STATUS_REPEAT = 1;

	private ProgressDialog diagDialog; // TODO

	private Handler handler;

	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.main);

		logWindow = (TextView) findViewById(R.id.logwindow);

		// 判断是否需要更新hosts文件
		int appStatus = appStatus();

		// if (appStatus == APP_STATUS_REPEAT && hasFile("/system/etc/hosts",
		// 200)) {
		// logWindow.append(getString(R.string.MSG_DNS_NONEED_UPDATE));
		// } else {
		// if (appStatus == APP_STATUS_NEW)
		// logWindow.append(getString(R.string.MSG_FISRT_TIME));
		//
		// logWindow.append(getString(R.string.MSG_DNS_FILES_UPDATE));
		// int result = Utils.rootCMD(getString(R.string.CMDremount));
		// if (result != 0) {
		// logWindow.append(getString(R.string.ERR_NO_ROOT));
		// } else {
		// installFiles("/system/etc/hosts", R.raw.basichosts, null);
		// logWindow.append(getString(R.string.MSG_INSTALL_COMPLETED));
		// }
		// }
		
		if (appStatus == APP_STATUS_NEW)
			logWindow.append(getString(R.string.MSG_FISRT_TIME));

	}

	@Override
	protected void onStart() {
		super.onStart();

		if (Utils.isCmwap(this))
			serviceLevel = Config.getServiceLevel(this);
		else
			serviceLevel = WrapService.SERVER_LEVEL_STOP;

		Logger.d(TAG, "服务级别为：" + serviceLevel);

		redrawButton();
	}

	public void onClick(View v) {

		Intent serviceIn = new Intent(this, WrapService.class);

		int message = R.string.serviceTagUp;

		switch (v.getId()) {

//		case R.id.Switch:
//
//			if (serviceLevel != WrapService.SERVER_LEVEL_NULL) {
//				stopService(serviceIn);
//				Logger.i(TAG, "禁用服务");
//				// Utils.rootCMD(getString(R.string.CMDiptablesDisable));
//				Toast.makeText(this, R.string.serviceTagDown,
//						Toast.LENGTH_SHORT).show();
//				serviceLevel = WrapService.SERVER_LEVEL_NULL;
//				Utils.rootCMD(getString(R.string.CMDiptablesDisable));
//				Config.saveServiceLevel(this, serviceLevel);
//				Config.setIptableStatus(this, false);
//				redrawButton();
//				return;
//			} else {
//				serviceLevel = WrapService.SERVER_LEVEL_BASE;
//			}
//			break;

		case R.id.BaseService:
			if (serviceLevel == WrapService.SERVER_LEVEL_BASE) {

				if (PreferenceManager.getDefaultSharedPreferences(this)
						.getBoolean("ULTRAMODE", false) == true)
					serviceLevel = WrapService.SERVER_LEVEL_FROGROUND_SERVICE;
				else
					serviceLevel = WrapService.SERVER_LEVEL_APPS;
				message = R.string.serviceTagApp;
			} else {
				serviceLevel = WrapService.SERVER_LEVEL_BASE;
			}

			break;
		}

		serviceIn.putExtra("SERVERLEVEL", serviceLevel);
		Logger.i(TAG, "启用服务");
		startService(serviceIn);
		Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
		redrawButton();
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_ABOUT_ID:
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setView(
					LayoutInflater.from(this).inflate(R.layout.about, null))
					.setIcon(R.drawable.icon).setTitle(R.string.MENU_ABOUT)
					.setPositiveButton(R.string.OKOK,
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int id) {
									dialog.cancel();
								}
							});

			AlertDialog dialog = builder.create();
			return dialog;
		case DIALOG_TEST_ID:
			diagDialog = new ProgressDialog(this);
			diagDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			diagDialog.setMessage("测试中……");
			TestManager manager = new TestManager(handler);
			manager.start();
			return diagDialog;
		default:
			return null;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.options_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.TEST:
			logWindow.setText("");

			handler = new Handler() {
				int progress = 0;

				@Override
				public void handleMessage(Message msg) {
					progress = msg.getData().getInt("PROGRESS");
					if (progress >= 100) {
						diagDialog.setProgress(0);
						dismissDialog(DIALOG_TEST_ID);
					} else {
						diagDialog.setProgress(progress);
					}
					String testName = msg.getData().getString("TESTNAME");
					String diaMsg = msg.getData().getString("MESSAGE");
					String errMsg = msg.getData().getString("ERRMSG");
					Logger.d(TAG, "testName: " + testName);
					diagDialog.setMessage(diaMsg);
					if (errMsg != null) {
						logWindow.append(errMsg);
					} else {
						logWindow.append(testName
								+ getString(R.string.TEST_PASSED));
					}
				}
			};
			proxyHost = Config.getStringPref(this, "PROXYHOST", "10.0.0.172");
			proxyPort = Integer.parseInt(Config.getStringPref(this,
					"PROXYPORT", "80"));
			DNSServer = Config.getStringPref(this, "DNSADD", "");
			showDialog(DIALOG_TEST_ID);
			return true;

		case R.id.SETTING:
			startActivityForResult(new Intent(this, Config.class), 0);
			return true;

		case R.id.ABOUT:
			showDialog(DIALOG_ABOUT_ID);
			return true;

		}
		return false;
	}

	/**
	 * 判断程序安装状态
	 * 
	 * @return -1 初次安装 0 升级安装 1 同版本再次安装
	 */
	private int appStatus() {
		int firsTime = APP_STATUS_NEW;
		// 判断状态
		try {
			SharedPreferences pref = PreferenceManager
					.getDefaultSharedPreferences(this);
			int ver = pref.getInt("VERSION", APP_STATUS_NEW);
			PackageManager pm = getPackageManager();
			PackageInfo pi;

			pi = pm.getPackageInfo(getPackageName(), APP_STATUS_UPDATE);
			int newVer = pi.versionCode;

			if (ver == newVer) {
				firsTime = APP_STATUS_REPEAT;
			} else {
				if (ver != APP_STATUS_NEW)
					firsTime = APP_STATUS_UPDATE;
				tag(newVer);
			}

		} catch (NameNotFoundException e) {
			Logger.e(TAG, e.getLocalizedMessage());
		}
		return firsTime;
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

			File destF = new File(dest);

			// 如果文件不存在，则随便touch一个先
			if (!destF.exists())
				Utils.rootCMD("busybox touch " + dest);

			Utils.rootCMD("chmod 666 " + dest);

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
			Logger.e(TAG, "未发现目的路径", e);
		} catch (IOException e) {
			Logger.e(TAG, "安装文件错误", e);
		}
		return result;
	}

	/**
	 * 判断是否存在指定文件
	 * 
	 * @param file
	 *            文件绝对路径名
	 * @param length
	 *            指定文件长度
	 * @return
	 */
	private boolean hasFile(String file, int length) {
		boolean result = false;
		File dstFile = new File(file);
		if (dstFile.length() > length)
			result = true;

		return result;
	}

	private void redrawButton() {

//		ToggleButton switcher = (ToggleButton) findViewById(R.id.Switch);
//		switcher.setOnClickListener(this);

		ToggleButton baseServiceSwitcher = (ToggleButton) findViewById(R.id.BaseService);
		baseServiceSwitcher.setOnClickListener(this);

		switch (serviceLevel) {

		case WrapService.SERVER_LEVEL_BASE:
			//switcher.setChecked(true);
			baseServiceSwitcher.setEnabled(true);
			baseServiceSwitcher.setChecked(false);
			break;

		case WrapService.SERVER_LEVEL_APPS:
		case WrapService.SERVER_LEVEL_FROGROUND_SERVICE:
		//	switcher.setChecked(true);
			baseServiceSwitcher.setEnabled(true);
			baseServiceSwitcher.setChecked(true);
			break;

		case WrapService.SERVER_LEVEL_NULL:
			baseServiceSwitcher.setEnabled(false);
			baseServiceSwitcher.setChecked(false);
			break;

		default:
			baseServiceSwitcher.setEnabled(false);
		//	switcher.setEnabled(false);
		}
	}

	/**
	 * 更新版本号
	 */
	private void tag(int newVer) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = pref.edit();
		editor.putInt("VERSION", newVer);
		editor.commit();
	}

	private class TestManager extends Thread {

		Handler handler = null;

		TestManager(Handler handler) {
			this.handler = handler;

		}

		@Override
		public void run() {
			Message msg = handler.obtainMessage();
			Bundle bundle = new Bundle();

			int result = Utils.rootCMD("iptables -L -t nat");
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
			if (result == 127) { // 没有iptables
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
			if (serviceLevel == WrapService.SERVER_LEVEL_STOP) {
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
				bundle.putString("ERRMSG",
						getString(R.string.ERR_UNSUPPORT_HTTPS));
			}
			channel.destory();
			bundle.putInt("PROGRESS", 60);
			bundle.putString("MESSAGE", getString(R.string.TEST_OTHER));
			msg.setData(bundle);
			handler.sendMessage(msg);

			// 测试DNS
			msg = handler.obtainMessage();
			bundle.putString("TESTNAME", getString(R.string.TEST_DNS));
			channel = new WapChannel(null, DNSServer + ":53", proxyHost,
					proxyPort);
			testSleep(5000);
			if (!channel.isConnected()) {
				bundle.putString("ERRMSG",
						getString(R.string.ERR_UNSUPPORT_DNS));
			}
			channel.destory();
			bundle.putInt("PROGRESS", 80);
			bundle.putString("MESSAGE", getString(R.string.TEST_OTHER));
			msg.setData(bundle);
			handler.sendMessage(msg);

			// 测试Gtalk
			msg = handler.obtainMessage();
			bundle.putString("TESTNAME", getString(R.string.TEST_OTHER));
			channel = new WapChannel(null, "mtalk.google.com:5228", proxyHost,
					proxyPort);
			testSleep(5000);
			if (!channel.isConnected()) {
				bundle.putString("ERRMSG",
						getString(R.string.ERR_UNSUPPORT_OTHERS));
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