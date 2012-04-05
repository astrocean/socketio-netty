package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * 
 * @author yongboy
 * @time 2012-3-28
 * @version 1.0
 */
public class FlashSocketIO extends WebSocketIO {
	public FlashSocketIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super(ctx, req, uID);
	}
}