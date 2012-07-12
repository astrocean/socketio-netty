package com.yongboy.socketio.server.transport;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import com.yongboy.socketio.server.IOHandler;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Transports;

/**
 * 
 * @author yongboy
 * @time 2012-5-29
 * @version 1.0
 */
public class WebSocketIO extends GenericIO {
	private static final Logger log = Logger.getLogger(WebSocketIO.class);

	public WebSocketIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super(ctx, req, uID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.client.GenericIOClient#heartbeat()
	 */
	@Override
	public void heartbeat(final IOHandler handler) {
		prepareHeartbeat();
		scheduleClearTask(handler);

		// 25秒为默认触发值，但触发之后，客户端会发起新的一个心跳检测连接
		SocketIOManager.schedule(new Runnable() {
			@Override
			public void run() {
				Channel chan = ctx.getChannel();
				if (chan.isOpen()) {
					chan.write(new TextWebSocketFrame("2::"));
				}

				log.debug("emitting heartbeat for client " + getSessionID());
			}
		});
	}

	@Override
	public void sendEncoded(String message) {
		this.queue.offer(message);
		if (!this.open)
			return;

		while (true) {
			String msg = this.queue.poll();
			if (msg == null)
				break;

			log.debug("websocket writing " + msg + " for client "
					+ getSessionID());
			Channel chan = ctx.getChannel();
			if (chan.isOpen()) {
				chan.write(new TextWebSocketFrame(msg));
			}
		}
	}

	public void sendDirect(String message) {
		if (!this.open) {
			log.debug("this.open is false");
			return;
		}

		log.debug("websocket writing " + message + " for client "
				+ getSessionID());
		Channel chan = ctx.getChannel();
		if (chan.isOpen()) {
			chan.write(new TextWebSocketFrame(message));
		} else {
			log.debug("chan.isOpen() is false");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getId()
	 */
	@Override
	public String getId() {
		return Transports.WEBSOCKET.getValue();
	}
}