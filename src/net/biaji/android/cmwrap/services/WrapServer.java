package net.biaji.android.cmwrap.services;

import java.io.IOException;

public abstract class WrapServer extends Thread {

	public abstract boolean isClosed();

	public abstract void close() throws IOException;

	public abstract int getServPort();

	public abstract void setProxyHost(String host);

	public abstract void setProxyPort(int port);

	/**
	 * 设置此服务的目的地址
	 * 
	 * @param dest
	 */
	public abstract void setDest(String dest);
}
