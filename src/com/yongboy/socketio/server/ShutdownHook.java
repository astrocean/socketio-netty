package com.yongboy.socketio.server;

import com.yongboy.socketio.MainServer;

public class ShutdownHook extends java.lang.Thread {

	private MainServer server;

	public ShutdownHook(MainServer mainServer) {
		this.server = mainServer;
	}

	@Override
	public void run() {
		server.stop();
	}
}