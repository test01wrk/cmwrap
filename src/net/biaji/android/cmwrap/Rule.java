package net.biaji.android.cmwrap;

public class Rule {
	public final static int MODE_BASE = 0;
	public final static int MODE_SERV = 1;
	public final static int MODE_ALL_FORWARD_TO_ONE = 2;

	public String name;
	public int mode = MODE_BASE;
	public String desHost;
	public int desPort;
	public int servPort;
}
