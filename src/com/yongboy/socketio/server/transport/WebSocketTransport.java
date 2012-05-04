package com.yongboy.socketio.server.transport;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;

import com.yongboy.socketio.server.IOHandlerAbs;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Transports;

public class WebSocketTransport extends ITransport {
	private static final Logger log = Logger
			.getLogger(WebSocketTransport.class);
	private WebSocketServerHandshaker handshaker;

	public WebSocketTransport(IOHandlerAbs handler, HttpRequest req) {
		super(handler, req);
	}

	@Override
	public String getId() {
		return Transports.WEBSOCKET.getValue();
	}

	@Override
	protected GenericIO doPrepareI0Client(ChannelHandlerContext ctx,
			HttpRequest req, String sessionId) {
		WebSocketIO client = new WebSocketIO(ctx, req, sessionId);
		client.connect(null);
		client.heartbeat(this.handler);
		return client;
	}

	@Override
	public void doHandle(ChannelHandlerContext ctx, HttpRequest req,
			MessageEvent e) {
		log.debug("websocket handls the request ...");
		// 需要调用父级的，否则将会发生异常
		String sessionId = super.getSessionId();
		log.debug("session id " + sessionId);

		WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
				this.getTargetLocation(req, sessionId), null, false);
		this.handshaker = wsFactory.newHandshaker(req);
		if (this.handshaker == null) {
			wsFactory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
			return;
		}

		this.handshaker.handshake(ctx.getChannel(), req);

		doPrepareClient(ctx, req, sessionId);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-5-4
	 *
	 * @param ctx
	 * @param req
	 * @param sessionId
	 */
	private void doPrepareClient(ChannelHandlerContext ctx, HttpRequest req,
			String sessionId) {
		GenericIO client = null;
		try {
			client = (GenericIO) SocketIOManager.getDefaultStore().get(
					sessionId);
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		if (client != null) {
			return;
		}

		log.debug("the client is null now ...");
		client = doPrepareI0Client(ctx, req, sessionId);

		SocketIOManager.getDefaultStore().add(sessionId, client);
		this.handler.OnConnect(client);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.yongboy.socketio.client.ITransport#doHandle(org.jboss.netty.channel
	 * .ChannelHandlerContext,
	 * org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame,
	 * org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void doHandle(ChannelHandlerContext ctx, WebSocketFrame frame,
			MessageEvent e) {
		log.debug("frame " + frame + " with instance " + frame.getClass());
		if (frame instanceof CloseWebSocketFrame) {
			this.handshaker
					.close(ctx.getChannel(), (CloseWebSocketFrame) frame);
			return;
		} else if (frame instanceof PingWebSocketFrame) {
			ctx.getChannel().write(
					new PongWebSocketFrame(frame.getBinaryData()));
			return;
		} else if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format(
					"%s frame types not supported", frame.getClass().getName()));
		}

		TextWebSocketFrame textFrame = ((TextWebSocketFrame) frame);
		String content = textFrame.getText();
		log.debug("websocket received data packet " + content);

		GenericIO client = initGenericClient(ctx, null);
		if (client == null) {
			return;
		}

		String respContent = handleContent(client, content);
		log.debug("respContent " + respContent);

		client.sendEncoded(respContent);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-30
	 * 
	 * @return
	 */
	@Override
	protected String getSessionId(HttpRequest req) {
		String webSocketUrl = this.handshaker.getWebSocketUrl();
		log.debug("webSocketUrl " + webSocketUrl);
		log.debug("webSocketUrl sessionid "
				+ webSocketUrl.substring(webSocketUrl.lastIndexOf('/') + 1));

		return webSocketUrl.substring(webSocketUrl.lastIndexOf('/') + 1);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-30
	 * 
	 * @param req
	 * @param sessionId
	 * @return
	 */
	private String getTargetLocation(HttpRequest req, String sessionId) {
		return "ws://" + req.getHeader(HttpHeaders.Names.HOST)
				+ "/socket.io/1/" + getId() + "/" + sessionId;
	}
}