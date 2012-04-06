package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.Transports;

public class XhrPollingTransport extends ITransport {

	public XhrPollingTransport(IOHandlerAbs handler, HttpRequest req) {
		super(handler, req);
	}

	public static String getName() {
		return Transports.XHRPOLLING.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.client.ITransport#getId()
	 */
	@Override
	public String getId() {
		return Transports.XHRPOLLING.getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.yongboy.socketio.server.transport.ITransport#initGenericClient(org
	 * .jboss.netty.channel.ChannelHandlerContext,
	 * org.jboss.netty.handler.codec.http.HttpRequest)
	 */
	@Override
	protected GenericIO initGenericClient(ChannelHandlerContext ctx,
			HttpRequest req) {
		GenericIO client = super.initGenericClient(ctx, req);

		if (!(client instanceof XhrIO)) {
			String sessionId = super.getSessionId();
			super.store.remove(sessionId);

			return initGenericClient(ctx, req);
		}

		// 需要切换到每一个具体的transport中
		if (req.getMethod() == HttpMethod.GET) { // 非第一次请求时
			client.reconnect(ctx, req);

			client.heartbeat(this.handler);
		}

		return client;
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
		XhrIO client = new XhrIO(ctx, req, sessionId);
		client.prepare();
		client.connect(null);

		return client;
	}
}