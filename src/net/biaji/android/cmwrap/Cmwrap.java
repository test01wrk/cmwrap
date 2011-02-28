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

import net.biaji.android.cmwrap.services.WrapService;
import net.biaji.android.cmwrap.utils.Utils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
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

    private final String TAG = "CMWRAP->";

    private final int DIALOG_ABOUT_ID = 0;

    private final int APP_STATUS_NEW = -1;

    private final int APP_STATUS_UPDATE = 0;

    private final int APP_STATUS_REPEAT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        logWindow = (TextView) findViewById(R.id.logwindow);

        // 判断是否需要更新hosts文件
        int appStatus = appStatus();

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

            case R.id.BaseService:

                if (serviceLevel != WrapService.SERVER_LEVEL_NULL) {
                    stopService(serviceIn);
                    Logger.i(TAG, "禁用服务");
                    serviceLevel = WrapService.SERVER_LEVEL_NULL;
                    Config.saveServiceLevel(this, serviceLevel);
                    // Config.setIptableStatus(this, false);
                    message = R.string.serviceTagDown;
                } else {
                    Logger.i(TAG, "启用服务");
                    serviceLevel = WrapService.SERVER_LEVEL_APPS;
                    serviceIn.putExtra("SERVERLEVEL", serviceLevel);
                    startService(serviceIn);
                    message = R.string.serviceTagApp;
                }

                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                redrawButton();

                break;
        }

    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ABOUT_ID:
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setView(LayoutInflater.from(this).inflate(R.layout.about, null))
                        .setIcon(R.drawable.icon).setTitle(R.string.MENU_ABOUT)
                        .setPositiveButton(R.string.OKOK, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

                AlertDialog dialog = builder.create();
                return dialog;
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
                startActivity(new Intent(this, TestActivity.class));
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
            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
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

    private void redrawButton() {

        // ToggleButton switcher = (ToggleButton) findViewById(R.id.Switch);
        // switcher.setOnClickListener(this);

        ToggleButton baseServiceSwitcher = (ToggleButton) findViewById(R.id.BaseService);
        baseServiceSwitcher.setOnClickListener(this);

        switch (serviceLevel) {

            case WrapService.SERVER_LEVEL_BASE:
                // switcher.setChecked(true);
                baseServiceSwitcher.setEnabled(true);
                baseServiceSwitcher.setChecked(false);
                break;

            case WrapService.SERVER_LEVEL_APPS:
            case WrapService.SERVER_LEVEL_FROGROUND_SERVICE:
                // switcher.setChecked(true);
                baseServiceSwitcher.setEnabled(true);
                baseServiceSwitcher.setChecked(true);
                break;

            case WrapService.SERVER_LEVEL_NULL:
                // baseServiceSwitcher.setEnabled(false);
                baseServiceSwitcher.setChecked(false);
                break;

            default:
                baseServiceSwitcher.setEnabled(false);
                // switcher.setEnabled(false);
        }
    }

    /**
     * 更新版本号
     */
    private void tag(int newVer) {
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("VERSION", newVer);
        editor.commit();
    }

}
