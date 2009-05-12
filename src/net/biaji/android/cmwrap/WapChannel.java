package net.biaji.android.cmwrap;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

public class WapChannel extends Thread {

	private long starTime;
	
	private int 心跳 = 10;
	
	private boolean isConnected = false;
	
	private Socket orgSocket;
	
	private Socket innerSocket;
	
	public WapChannel(Socket socket){
		this.orgSocket = socket;
	}

	@Override
	public void run() {
		InetSocketAddress remoteAddr =(InetSocketAddress) orgSocket.getRemoteSocketAddress();
		
	}

}
