package com.yongboy.socketio.server;

import com.yongboy.socketio.server.transport.IOClient;

/**
 * 
 * @author yongboy
 * @time 2012-3-23
 * @version 1.0
 */
interface IOHandler {
	void OnConnect(IOClient client);

	void OnMessage(IOClient client, String oriMessage);

	void OnDisconnect(IOClient client);

	void OnShutdown();
}
