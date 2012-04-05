package com.yongboy.socketio.test;

import com.yongboy.socketio.MainServer;

/**
 * 
 * @author nieyong
 * @time 2012-3-27
 * @version 1.0
 */
public class ChatServer {

	public static void main(String[] args) {
		MainServer chatServer = new MainServer(new DemoChatHandler(),
				9000);
		chatServer.start();
	}
}