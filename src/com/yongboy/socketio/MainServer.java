package com.yongboy.socketio;

import org.apache.log4j.Logger;

import com.yongboy.socketio.flash.FlashSecurityServer;
import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.ShutdownHook;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.SocketIOServer;

/**
 * 
 * @author nieyong
 * @time 2012-3-29
 * @version 1.0
 */
public class MainServer {
	private static final Logger log = Logger.getLogger(MainServer.class);
	private IOHandlerAbs handler;
	private int socketIOPort;
	private int flashSecurityPort = SocketIOManager.option.flash_policy_port;

	private SocketIOServer socketIOServer;
	private FlashSecurityServer flashSecurityServer;

	public MainServer(IOHandlerAbs handler, int socketIOPort,
			int flashSecurityPort) {
		this.handler = handler;
		this.socketIOPort = socketIOPort;
		this.flashSecurityPort = flashSecurityPort;
	}

	public MainServer(IOHandlerAbs handler, int socketIOPort) {
		this.handler = handler;
		this.socketIOPort = socketIOPort;
	}

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-29
	 * 
	 */
	public void start() {
		log.info("start the SocketIOServer with port : " + socketIOPort);
		socketIOServer = new SocketIOServer(handler, socketIOPort);
		socketIOServer.start();

		if (SocketIOManager.option.flash_policy_server) {
			log.info("start the FlashSecurityServer with port : "
					+ flashSecurityPort);
			flashSecurityServer = new FlashSecurityServer(flashSecurityPort);
			flashSecurityServer.start();
		}

		log.info("add ShutDownHook now ...");
		Runtime.getRuntime().addShutdownHook(new ShutdownHook(this));
	}

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-29
	 * 
	 */
	public void stop() {
		if (socketIOServer != null)
			socketIOServer.stop();
		if (flashSecurityServer != null) {
			flashSecurityServer.stop();
		}
	}
}