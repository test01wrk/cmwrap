package net.biaji.android.cmwrap.test;

import net.biaji.android.cmwrap.services.WapChannel;
import junit.framework.TestCase;

public class TestWapChannel extends TestCase {

	private String host = "58.213.152.15"; //江苏省南京市 电信ADSL

	private int port = 3128;
	
	private WapChannel target;


	public void testWapChannelSocketStringInt() {
		target = new WapChannel(null, host, port);
		assert(target.isConnected());
	}

}
