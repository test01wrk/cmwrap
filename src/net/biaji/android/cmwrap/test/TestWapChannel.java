package net.biaji.android.cmwrap.test;

import java.net.Socket;

import net.biaji.android.cmwrap.services.WapChannel;
import android.test.AndroidTestCase;

public class TestWapChannel extends AndroidTestCase {

	private String host = "58.213.152.15"; // 江苏省南京市 电信ADSL

	private int port = 3128;

	private WapChannel target;

	private Socket org;

	protected void setUp() throws Exception {

	}

	public void testWapChannelSocketStringInt() {
		target = new WapChannel(null, host, port);
		assert (target.isConnected());
	}

}
