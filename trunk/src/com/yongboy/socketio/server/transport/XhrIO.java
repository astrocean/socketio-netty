package com.yongboy.socketio.server.transport;

import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.CharsetUtil;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timer;

import com.yongboy.socketio.server.IOHandler;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Transports;

/**
 * 
 * @author yongboy
 * @time 2012-5-29
 * @version 1.0
 */
public class XhrIO extends GenericIO {
	private static final Logger log = Logger.getLogger(XhrIO.class);
	private static final Timer timer = new HashedWheelTimer();

	public XhrIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super(ctx, req, uID);

		// 添加超时
		ctx.getPipeline()
				.addFirst("timeout", new ReadTimeoutHandler(timer, 20));
	}

	private void _write(String message) {
		HttpResponse res = SocketIOManager.getInitResponse(req);

		res.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");

		res.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
		setContentLength(res, res.getContent().readableBytes());

		res.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);

		Channel chan = ctx.getChannel();
		if (chan.isOpen()) {
			ChannelFuture f = chan.write(res);
			f.addListener(ChannelFutureListener.CLOSE);
		} else {
			this.queue.offer(message);
		}
	}

	@Override
	public void sendEncoded(String message) {
		log.debug("the message is " + message + " been into queue!");
		this.queue.offer(message);
	}

	private void sendDirectMessage(String message) {
		if (this.open) {
			log.debug("send message is " + message);
			try {
				_write(message);
			} catch (Exception e) {
				e.printStackTrace();
				this.queue.offer(message);
			}
			open = false;
		} else {
			log.debug("this.open is false, push the message into queue");
			this.queue.offer(message);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.client.GenericIOClient#heartbeat()
	 */
	@Override
	public void heartbeat(final IOHandler handler) {
		if (!this.open) {
			scheduleClearTask(handler);
			return;
		}

		prepareHeartbeat();

		Channel chan = ctx.getChannel();
		if (!chan.isOpen()) {
			this.open = false;
			scheduleClearTask(handler);
			return;
		}

		HttpResponse res = SocketIOManager.getInitResponse(req);
		res.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
		res.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);

		chan.write(res);

		// 若无消息，则阻塞，直到返回false
		String message = null;
		try {
			message = queue.poll(19996L, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (message == null) {
			message = "8::";
		}
		try {
			chan.write(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8))
					.addListener(ChannelFutureListener.CLOSE);
		} catch (Exception e) {
			log.debug("java.io.IOException: 远程主机强迫关闭了一个现有的连接。");
			e.printStackTrace();
		}

		scheduleClearTask(handler);
	}

	@Override
	public void connect(String message) {
		if (message == null)
			sendDirectMessage("1::");
		else
			sendDirectMessage(message);
	}

	@Override
	public void disconnect() {
		super.disconnect();

		this.open = false;
	}

	@Override
	public void disconnect(String info) {
		super.disconnect(info);

		this.open = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getId()
	 */
	@Override
	public String getId() {
		return Transports.XHRPOLLING.getValue();
	}
}