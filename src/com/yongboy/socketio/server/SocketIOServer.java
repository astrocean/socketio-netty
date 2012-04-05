package com.yongboy.socketio.server;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * 调用入口
 * 
 * @author yongboy
 * @time 2012-3-23
 * @version 1.0
 */
public class SocketIOServer {
	private Logger log = Logger.getLogger(getClass());

	private ServerBootstrap bootstrap;
	private Channel serverChannel;
	private int port;
	private boolean running;
	private IOHandlerAbs handler;

	public SocketIOServer(IOHandlerAbs handler, int port) {
		this.port = port;
		this.handler = handler;
		this.running = false;		
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		// Set up the event pipeline factory.
		bootstrap.setPipelineFactory(new ServerPipelineFactory(handler));

		bootstrap.setOption("child.reuseAddress", true);
		// bootstrap.setOption("child.tcpNoDelay", true);
		// bootstrap.setOption("child.keepAlive", true);

		// Bind and start to accept incoming connections.
		this.serverChannel = bootstrap.bind(new InetSocketAddress(port));
		this.running = true;
		
		log.info("Server Started at port [" + port + "]");
	}

	public void stop() {
		if (!this.running)
			return;

		log.info("Server shutting down.");
		this.handler.OnShutdown();
		this.serverChannel.close();
		this.bootstrap.releaseExternalResources();
		log.info("**SHUTDOWN**");
		this.running = false;
	}
}