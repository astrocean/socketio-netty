package com.yongboy.socketio.test;

import com.yongboy.socketio.MainServer;

/**
 * 在线画报socket.io服务器端示范
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public class WhiteboardServer {
	public static void main(String[] args) {
		int port = 80;

		String envPort = System.getenv("VCAP_APP_PORT");
		if (envPort != null && envPort.trim().length() > 0) {
			port = Integer.parseInt(envPort.trim());
		}

		MainServer mainServer = new MainServer(new WhiteboardHandler(), port);
		mainServer.start();
	}
}