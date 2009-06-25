package net.biaji.android.cmwrap;

import java.util.ArrayList;

public class Config {

	public static enum SERVER_LEVEL {
		NULL, STOP, BASE, APPS, MORE_APPS
	};

	private static SERVER_LEVEL serverLevel = SERVER_LEVEL.BASE;

	private static String proxyServer = "";

	private static ArrayList<Rule> rules = new ArrayList<Rule>();

	private static Config config = new Config();

	private Config() {

	}
	
	public Config getInstance(){
		return config;
	}

	public static SERVER_LEVEL getServerLevel() {
		return serverLevel;
	}

	public static void setServerLevel(SERVER_LEVEL serverLevel) {
		Config.serverLevel = serverLevel;
	}

	public static String getProxyServer() {
		return proxyServer;
	}

	public static void setProxyServer(String proxyServer) {
		Config.proxyServer = proxyServer;
	}
	
	public void addRule(Rule rule){
		
	}
	
	public void removeRule(Rule rule){
		
	}

}
