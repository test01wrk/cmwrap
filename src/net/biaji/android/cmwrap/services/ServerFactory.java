package net.biaji.android.cmwrap.services;

import net.biaji.android.cmwrap.Rule;

public class ServerFactory {

	public static WrapServer getServer(Rule rule) {
		WrapServer server = null;
		if (rule.protocol.equals("tcp")) {
			server = new NormalTcpServer(rule.name, rule.servPort);
			server.setTarget(rule.desHost + ":" + rule.desPort);
		} else {
			server = new DNSServer(rule.name, rule.servPort);
		}

		return server;
	}

}
