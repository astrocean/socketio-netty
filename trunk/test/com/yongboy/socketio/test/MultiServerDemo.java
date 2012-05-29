package com.yongboy.socketio.test;

import com.yongboy.socketio.MainServer;

/**
 * 在线画报socket.io服务器端示范
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public class MultiServerDemo {
	public static void main(String[] args) {
		int port = 9000;

		/**
		 * 兼容cloudfoudry平台
		 */
		String envPort = System.getenv("VCAP_APP_PORT");
		if (envPort != null && envPort.trim().length() > 0) {
			port = Integer.parseInt(envPort.trim());
		}

		MainServer mainServer = new MainServer(port);
		mainServer.addNamespace("/whiteboard", new WhiteboardHandler());
		mainServer.addNamespace("/chat", new DemoChatHandler());
		mainServer.start();
	}
}