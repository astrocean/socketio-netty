package com.yongboy.socketio.flash;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

/**
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public class FlashSecurityServer {
	private Channel serverChannel;
	private ServerBootstrap bootstrap;
	private int port;

	public FlashSecurityServer(int port) {
		this.port = port;
		if (this.port < 0) {
			this.port = 10843;
		}
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-29
	 * 
	 */
	public void start() {
		bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
				Executors.newCachedThreadPool(),
				Executors.newCachedThreadPool()));

		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			private final Timer timer = new HashedWheelTimer();

			@Override
			public ChannelPipeline getPipeline() throws Exception {
				ChannelPipeline pipeline = Channels.pipeline();
				pipeline.addLast("timeout", new ReadTimeoutHandler(timer, 30));
				pipeline.addLast("decoder", new FlashSecurityDecoder());
				pipeline.addLast("handler", new FlashSecurityHandler());
				return pipeline;
			}
		});

		bootstrap.setOption("child.tcpNoDelay", true);
		bootstrap.setOption("child.keepAlive", true);

		// 设置为socket.io默认端口
		serverChannel = bootstrap.bind(new InetSocketAddress(port));
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-29
	 * 
	 */
	public void stop() {
		serverChannel.close();
		bootstrap.releaseExternalResources();
	}
}