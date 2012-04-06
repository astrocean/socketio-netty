package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.Transports;

public class HtmlfileTransport extends ITransport {

	public HtmlfileTransport(IOHandlerAbs handler) {
		super(handler);
	}

	public static String getName() {
		return Transports.HTMLFILE.getValue();
	}

	@Override
	public String getId() {
		return Transports.HTMLFILE.getValue();
	}

	/**
	 * 25秒为默认触发值，但触发之后，客户端会发起新的一个心跳检测连接
	 * 
	 * @param htmlfile
	 */

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
		HtmlfileIO client = new HtmlfileIO(ctx, req, sessionId);

		client.prepare();
		client.connect(null);

		client.heartbeat(this.handler);

		return client;
	}
}