package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.Transports;

public class JsonpPollingTransport extends ITransport {

	public JsonpPollingTransport(IOHandlerAbs handler, HttpRequest req) {
		super(handler, req);
	}

	public static String getName() {
		return Transports.JSONPP0LLING.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.client.ITransport#getId()
	 */
	@Override
	public String getId() {
		return Transports.JSONPP0LLING.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.yongboy.socketio.client.ITransport#initNewClient(org.jboss.netty.
	 * channel.ChannelHandlerContext,
	 * org.jboss.netty.handler.codec.http.HttpRequest, java.lang.String)
	 */
	@Override
	protected GenericIO doPrepareI0Client(ChannelHandlerContext ctx,
			HttpRequest req, String sessionId) {
		JsonpIO client = new JsonpIO(ctx, req, sessionId);
		client.prepare();
		client.connect(null);

		return client;
	}
}