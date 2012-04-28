package com.yongboy.socketio.test;

import com.yongboy.socketio.MainServer;

/**
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public class WhiteboardServer {

	public static void main(String[] args) {
		MainServer chatServer = new MainServer(new WhiteboardHandler(),
				9000);
		chatServer.start();
	}
}