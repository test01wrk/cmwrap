
package net.biaji.android.cmwrap.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import net.biaji.android.cmwrap.Config;
import net.biaji.android.cmwrap.Logger;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;

public class Utils {

    private final static String TAG = "Utils";

    private final static String LINEBREAK = System.getProperty("line.separator");

    public static String errMsg = "";

    public static String getErr() {
        return errMsg;
    }

    public static String bytesToHexString(byte[] bytes) {
        return bytesToHexString(bytes, 0, bytes.length);
    }

    public static String bytesToHexString(byte[] bytes, int start, int offset) {
        String result = "";

        String stmp = "";
        for (int n = start; n < offset; n++) {
            stmp = (Integer.toHexString(bytes[n] & 0XFF));
            if (stmp.length() == 1)
                result = result + "0" + stmp;
            else
                result = result + stmp;

        }
        return result.toUpperCase();
    }

    /**
     * 判断当前网络连接是否为cmwap
     * 
     * @param context
     * @return true 当前为cmwap连接<br>
     *         false 当前数据连接非cmwap
     */
    public static boolean isCmwap(Context context) {

        // 根据配置情况决定是否检查当前数据连接
        if (!Config.isCmwapOnly(context))
            return true;

        // -------------------

        ConnectivityManager manager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        if (networkInfo == null || networkInfo.getType() == ConnectivityManager.TYPE_WIFI)
            return false;
        boolean result = false;
        String currentConn = getCurrentDataConn(context);
        if (currentConn != null && currentConn.trim().equalsIgnoreCase("cmwap")) {
            result = true;
        }
        return result;
    }

    /**
     * 获取当前数据连接名称
     * 
     * @param context
     * @return 当前数据连接名称
     */
    public static String getCurrentDataConn(Context context) {
        String dataConn = "";

        Cursor mCursor = context.getContentResolver().query(
                Uri.parse("content://telephony/carriers"), new String[] {
                    "apn"
                }, "current=1", null, null);
        if (mCursor != null) {
            try {
                if (mCursor.moveToFirst()) {
                    dataConn = mCursor.getString(0);
                }
            } catch (Exception e) {
                Logger.e(TAG, "Can not get Network info", e);
            } finally {
                mCursor.close();
            }
        }
        return dataConn;
    }

    /**
     * 重新设置DNS服务器地址
     * 
     * @param dns 服务器地址
     */
    public static void flushDns(String dns) {
        // String getdns = "getprop | grep net.dns1";
        String setcmd = "setprop net.dns1 ";
        rootCMD(setcmd + dns);
    }

    /**
     * 以root权限执行命令
     * 
     * @param 需要执行的指令
     * @return -1 执行失败<br>
     *         0 执行正常
     */
    public static int rootCMD(String cmd) {
        int result = -1;
        DataOutputStream os = null;
        InputStream err = null, out = null;
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
            err = process.getErrorStream();
            BufferedReader bre = new BufferedReader(new InputStreamReader(err), 1024 * 8);

            out = process.getInputStream();

            os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(cmd + LINEBREAK);
            os.flush();
            os.writeBytes("exit" + LINEBREAK);
            os.flush();

            String resp;
            while ((resp = bre.readLine()) != null) {
                Logger.d(TAG, resp);
                errMsg = resp;
            }
            result = process.waitFor();
            if (result == 0)
                Logger.d(TAG, cmd + " exec success");
            else {
                Logger.d(TAG, cmd + " exec with result " + result);
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
                if (process != null)
                    ;
            } catch (IOException e) {
            }

        }

        return result;
    }

    public static byte[] int2byte(int res) {
        byte[] targets = new byte[4];

        targets[0] = (byte) (res & 0xff);// 最低位
        targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
        targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
        return targets;
    }

    /**
     * 判断是否存在指定文件
     * 
     * @param file 文件绝对路径名
     * @param length 指定文件长度
     * @return
     */
    public static boolean hasFile(String file, int length) {
        boolean result = false;
        File dstFile = new File(file);
        if (dstFile.length() > length)
            result = true;

        return result;
    }

    /**
     * 安装文件
     * 
     * @param dest 安装路径
     * @param resId 资源文件id
     * @param mod 文件属性
     * @return
     */
    public static int installFiles(Context context, String dest, int resId, String mod) {
        int result = -1;
        BufferedInputStream bin = null;
        FileOutputStream fo = null;
        try {
            bin = new BufferedInputStream(context.getResources().openRawResource(resId));

            if (mod == null)
                mod = "644";

            File destF = new File(dest);

            // 如果文件不存在，则随便touch一个先
            if (!destF.exists())
                rootCMD("busybox touch " + dest);

            rootCMD("chmod 666 " + dest);

            fo = new FileOutputStream(destF);
            int length;
            byte[] content = new byte[1024];

            while ((length = bin.read(content)) > 0) {
                fo.write(content, 0, length);
            }

            fo.close();
            bin.close();
            rootCMD("chmod " + mod + "  " + dest);
            result = 0;

        } catch (FileNotFoundException e) {
            Logger.e(TAG, "未发现目的路径", e);
        } catch (IOException e) {
            Logger.e(TAG, "安装文件错误", e);
        }
        return result;
    }
}
