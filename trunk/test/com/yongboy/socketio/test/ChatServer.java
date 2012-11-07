package com.yongboy.socketio.test;

import com.yongboy.socketio.MainServer;

/**
 * 存在一定BUG，不建议使用
 * Please do not use it now!
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
@Deprecated
public class ChatServer {

	public static void main(String[] args) {
		MainServer chatServer = new MainServer(new DemoChatHandler(), 80);
		chatServer.start();
	}
}