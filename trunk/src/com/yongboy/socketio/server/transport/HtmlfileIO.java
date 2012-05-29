package com.yongboy.socketio.server.transport;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.DefaultHttpChunk;
import org.jboss.netty.handler.codec.http.HttpChunk;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

import com.yongboy.socketio.server.IOHandler;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Transports;

/**
 * 
 * @author yongboy
 * @time 2012-5-29
 * @version 1.0
 */
public class HtmlfileIO extends GenericIO {
	private final static String TEMPLATE = "<script>_('%s');</script>";

	public HtmlfileIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super(ctx, req, uID);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.client.GenericIOClient#heartbeat()
	 */
	@Override
	public void heartbeat(final IOHandler handler) {
		prepareHearbeat();

		// 25秒为默认触发值，但触发之后，客户端会发起新的一个心跳检测连接
		SocketIOManager.schedule(new Runnable() {
			@Override
			public void run() {
				scheduleClearTask(handler);
				
				__write(String.format(TEMPLATE, "2::"));
			}
		});
	}

	private void __write(String message) {
		if (!this.open)
			return;

		Channel chan = ctx.getChannel();
		if (chan.isOpen()) {
			writeStringChunk(chan, message);
		}
	}

	private void _write(String message) {
		if (!this.open)
			return;

		HttpResponse response = SocketIOManager.getInitResponse(req);
		response.setChunked(true);
		response.setHeader(HttpHeaders.Names.CONTENT_TYPE,
				"text/html; charset=UTF-8");
		response.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
		response.setHeader(HttpHeaders.Names.TRANSFER_ENCODING,
				HttpHeaders.Values.CHUNKED);

		Channel chan = ctx.getChannel();
		chan.write(response);

		writeStringChunk(chan, message);
	}

	private void writeStringChunk(Channel channel, String data) {
		ChannelBuffer chunkContent = ChannelBuffers.dynamicBuffer(channel
				.getConfig().getBufferFactory());
		chunkContent.writeBytes(data.getBytes());
		HttpChunk chunk = new DefaultHttpChunk(chunkContent);

		channel.write(chunk);
	}

	@Override
	public void sendEncoded(String message) {
		this.queue.offer(message);
		if (!this.open){
			return;
		}
		
		while (true) {
			String msg = this.queue.poll();
			if (msg == null)
				break;
			
			__write(String.format(TEMPLATE, msg.replaceAll("\"", "\\\"")));
		}		
	}

	@Override
	public void prepare() {
		// 缓冲必须凑够256字节，浏览器端才能够正常接收 ...
		StringBuilder builder = new StringBuilder();
		builder.append("<html><body><script>var _ = function (msg) { parent.s._(msg, document); };</script>");
		int leftChars = 256 - builder.length();
		for (int i = 0; i < leftChars; i++) {
			builder.append(" ");
		}

		_write(builder.toString());
	}

	@Override
	public void disconnect(String info) {
		sendEncoded("0::");

		this.open = false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getId()
	 */
	@Override
	public String getId() {
		return Transports.HTMLFILE.getValue();
	}
}