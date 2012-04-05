package com.yongboy.socketio.server.transport;

import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;

import java.util.List;
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
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Transports;

public class JsonpIO extends GenericIO {
	private static final Logger log = Logger.getLogger(JsonpIO.class);
	private final static String TEMPLATE = "io.j[%s]('%s');";

	public JsonpIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super(ctx, req, uID);
	}

	private void _write(String message) {
		HttpResponse res = SocketIOManager.getInitResponse(req);

		res.addHeader(CONTENT_TYPE, "text/javascript; charset=UTF-8");

		res.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
		setContentLength(res, res.getContent().readableBytes());

		res.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
		res.addHeader("X-XSS-Protection", "0");

		Channel chan = ctx.getChannel();
		if (chan.isOpen()) {
			ChannelFuture f = chan.write(res);
			f.addListener(ChannelFutureListener.CLOSE);
		} else {
			queue.offer(message);
		}
	}

	@Override
	public void sendEncoded(String message) {
		this.queue.offer(message);
	}

	private void sendDirectMessage(String message) {
		if (!this.open) {
			this.queue.offer(message);
			return;
		}

		try {
			String iString = getTargetFormatMessage(message);
			_write(String.format(TEMPLATE, iString, message));
		} catch (Exception e) {
			log.info("Exception " + e.toString());
			e.printStackTrace();
			this.queue.offer(message);
		}

		open = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.client.GenericIOClient#heartbeat()
	 */
	@Override
	public void heartbeat() {
		if (!this.open) {
			scheduleClearTask();
			return;
		}

		prepareHearbeat();

		Channel chan = ctx.getChannel();
		if (!chan.isOpen()) {
			this.open = false;
			scheduleClearTask();
			return;
		}

		HttpResponse res = SocketIOManager.getInitResponse(req);
		res.addHeader(CONTENT_TYPE, "text/javascript; charset=UTF-8");
		res.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
		res.addHeader("X-XSS-Protection", "0");
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
		
		String templateMessage = getTargetFormatMessage(message);
		
		chan.write(ChannelBuffers.copiedBuffer(templateMessage, CharsetUtil.UTF_8))
				.addListener(ChannelFutureListener.CLOSE);

		scheduleClearTask();
	}

	private String getTargetFormatMessage(String message) {
		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
				req.getUri());

		List<String> paras = queryStringDecoder.getParameters().get("i");
		String iString = null;
		if (paras == null || paras.isEmpty()) {
			iString = "0";
		} else {
			iString = paras.get(0);
		}

		log.debug("format json message : "
				+ String.format(TEMPLATE, iString, message));
		
		return String.format(TEMPLATE, iString, message);
	}

	@Override
	public void connect(String message) {
		sendDirectMessage("1::");
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
		return Transports.JSONPP0LLING.getValue();
	}
}