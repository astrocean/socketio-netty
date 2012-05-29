package com.yongboy.socketio;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.yongboy.socketio.flash.FlashSecurityServer;
import com.yongboy.socketio.server.IOHandler;
import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.SocketIOServer;
import com.yongboy.socketio.server.transport.IOClient;

/**
 * 多个namespace/endpoint
 * 
 * @author yongboy
 * @time 2012-3-29
 * @version 1.0
 */
public class MainServer {
	private static final Logger log = Logger.getLogger(MainServer.class);
	private final static Map<String, IOHandlerAbs> handlerMap;
	private int socketIOPort;
	private int flashSecurityPort = SocketIOManager.option.flash_policy_port;

	private SocketIOServer socketIOServer;
	private FlashSecurityServer flashSecurityServer;

	static {
		handlerMap = new HashMap<String, IOHandlerAbs>();
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-5-28
	 * 
	 * @param client
	 * @return
	 */
	public static IOHandler getIOHandler(IOClient client) {
		if (client == null)
			return null;

		return handlerMap.get(client.getNamespace());
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-5-28
	 * 
	 * @return
	 */
	public static Collection<IOHandlerAbs> getAllHandlers() {
		return Collections.unmodifiableCollection(handlerMap.values());
	}

	public MainServer(IOHandlerAbs handler, int socketIOPort,
			int flashSecurityPort) {
		this(socketIOPort, flashSecurityPort);

		addNamespace("", handler);
	}

	public MainServer(IOHandlerAbs handler, int socketIOPort) {
		this(handler, socketIOPort, SocketIOManager.option.flash_policy_port);
	}

	public MainServer(int socketIOPort, int flashSecurityPort) {
		this(socketIOPort);
		this.flashSecurityPort = flashSecurityPort;
	}

	public MainServer(int socketIOPort) {
		this.socketIOPort = socketIOPort;
	}

	public void addNamespace(String namespace, IOHandlerAbs handler) {
		handlerMap.put(namespace, handler);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-29
	 * 
	 */
	public void start() {
		log.info("start the SocketIOServer with port : " + socketIOPort);
		socketIOServer = new SocketIOServer(socketIOPort);
		socketIOServer.start();

		if (SocketIOManager.option.flash_policy_server) {
			log.info("start the FlashSecurityServer with port : "
					+ flashSecurityPort);
			flashSecurityServer = new FlashSecurityServer(flashSecurityPort);
			flashSecurityServer.start();
		}

		log.info("add ShutDownHook now ...");
		Runtime.getRuntime().addShutdownHook(new ShutdownHooks(this));
	}

	/**
	 * 
	 * @author yongboy
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

	private static class ShutdownHooks extends java.lang.Thread {
		private MainServer server;

		public ShutdownHooks(MainServer server) {
			this.server = server;
		}

		@Override
		public void run() {
			server.stop();
		}
	}
}