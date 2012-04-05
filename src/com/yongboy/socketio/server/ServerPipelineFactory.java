package com.yongboy.socketio.server;

import static org.jboss.netty.channel.Channels.pipeline;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;

/**
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public class ServerPipelineFactory implements ChannelPipelineFactory {
	private IOHandlerAbs handler;

	public ServerPipelineFactory(IOHandlerAbs handler) {
		this.handler = handler;
	}

	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = pipeline();
		pipeline.addLast("decoder", new HttpRequestDecoder());
		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
		pipeline.addLast("encoder", new HttpResponseEncoder());

		pipeline.addLast("handler", new SocketIOTransportAdapter(handler));
		return pipeline;
	}
}