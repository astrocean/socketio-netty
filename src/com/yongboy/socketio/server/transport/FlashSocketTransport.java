package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.Transports;

/**
 * 
 * @author yongboy
 * @time 2012-3-29
 * @version 1.0
 */
public class FlashSocketTransport extends WebSocketTransport {

	public FlashSocketTransport(IOHandlerAbs handler) {
		super(handler);
	}

	public static String getName() {
		return Transports.FLASHSOCKET.getValue();
	}

	@Override
	public String getId() {
		return Transports.FLASHSOCKET.getValue();
	}

	@Override
	protected GenericIO doPrepareI0Client(ChannelHandlerContext ctx,
			HttpRequest req, String sessionId) {
		FlashSocketIO client = new FlashSocketIO(ctx, req, sessionId);
		client.connect(null);
		client.heartbeat();
		return client;
	}
}