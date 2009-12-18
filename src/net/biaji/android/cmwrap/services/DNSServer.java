package net.biaji.android.cmwrap.services;

import java.io.IOException;

public class DNSServer extends WrapServer {
	public DNSServer(String name, int port) {
		this(name, port, "10.0.0.172", 80);
	}

	public DNSServer(String name, int port, String proxyHost, int proxyPort) {
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int getServPort() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setDest(String dest) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setServPort(int port) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProxyHost(String host) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setProxyPort(int port) {
		// TODO Auto-generated method stub
		
	}
}
