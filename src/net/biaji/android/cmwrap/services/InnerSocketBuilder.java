package net.biaji.android.cmwrap.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;

import net.biaji.android.cmwrap.Logger;

public class InnerSocketBuilder {

	private String proxyHost = "10.0.0.172";
	private int proxyPort = 80;
	private String target = "";

	private long starTime = System.currentTimeMillis();
	private final String TAG = "CMWRAP->InnerSocketBuilder";
	private final String UA = "biAji's wap channel";

	public InnerSocketBuilder(String target) {
		this("10.0.0.172", 80, target);
	}

	public InnerSocketBuilder(String proxyHost, int proxyPort, String target) {
		this.proxyHost = proxyHost;
		this.proxyPort = proxyPort;
		this.target = target;

	}

	public Socket getSocket() {
		Socket innerSocket = null;

		// starTime = System.currentTimeMillis();
		Logger.v(TAG, "建立通道");
		BufferedReader din = null;
		BufferedWriter dout = null;

		try {
			innerSocket = new Socket(proxyHost, proxyPort);
			innerSocket.setKeepAlive(true);
			innerSocket.setSoTimeout(120 * 1000);

			din = new BufferedReader(new InputStreamReader(innerSocket
					.getInputStream()));
			dout = new BufferedWriter(new OutputStreamWriter(innerSocket
					.getOutputStream()));

			String connectStr = "CONNECT " + target
					+ " HTTP/1.1\r\nUser-agent: " + this.UA + "\r\n\r\n";

			dout.write(connectStr);
			dout.flush();
			Logger.v(TAG, connectStr);

			String result = din.readLine();
			String line = "";
			while ((line = din.readLine()) != null) {
				if (line.trim().equals(""))
					break;
				Logger.v(TAG, line);
			}

			if (result != null && result.contains("200")) {
				Logger.v(TAG, result);
				Logger.v(TAG, "通道建立成功， 耗时："
						+ (System.currentTimeMillis() - starTime) / 1000);
			} else {
				Logger.d(TAG, "建立隧道失败");
			}

		} catch (IOException e) {
			Logger.e(TAG, "建立隧道失败：" + e.getLocalizedMessage());
		}
		return innerSocket;
	}

}
