package com.yongboy.socketio.server;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;

import com.yongboy.socketio.server.transport.BlankIO;
import com.yongboy.socketio.server.transport.GenericIO;
import com.yongboy.socketio.server.transport.IOClient;
import com.yongboy.socketio.server.transport.ITransport;

public class SocketIOTransportAdapter extends SimpleChannelUpstreamHandler {
	private static final Logger log = Logger
			.getLogger(SocketIOTransportAdapter.class);

	private IOHandlerAbs handler;
	private ITransport currentTransport = null;

	public SocketIOTransportAdapter(IOHandlerAbs handler) {
		super();
		this.handler = handler;
	}

	private String getUniqueID() {
		return UUID.randomUUID().toString();
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			org.jboss.netty.channel.ChannelStateEvent e) throws Exception {
		if (this.currentTransport == null) {
			return;
		}

		log.debug("this.currentTransport.id " + this.currentTransport.getId());
		if ("websocket,flashsocket,htmlfile".contains(this.currentTransport
				.getId())) {
			log.debug("going to clear client~");
			Store store = SocketIOManager.getDefaultStore();
			String sessionId = this.currentTransport.getSessionId();
			IOClient client = store.get(sessionId);
			if (client == null) {
				log.debug("client had been removed by session id " + sessionId);
				return;
			}

			if (client instanceof GenericIO) {
				GenericIO genericIO = (GenericIO) client;
				genericIO.scheduleRemoveTask(this.handler);
			}
		}

		log.debug("client is not null");
	}

	public void disconnect(IOClient client) {
		client.disconnect();
		SocketIOManager.getDefaultStore().remove(client.getSessionID());

		handler.OnDisconnect(client);
	}

	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
			throws Exception {
		Object msg = e.getMessage();
		if (msg instanceof HttpRequest) {
			handleHttpRequest(ctx, (HttpRequest) msg, e);
			return;
		}

		if (msg instanceof WebSocketFrame) {
			if (currentTransport != null) {
				this.currentTransport.doHandle(ctx, (WebSocketFrame) msg, e);
			} else {
				log.warn("currentTransport is null, do nothing ...");
			}
		}
	}

	private void handleHttpRequest(ChannelHandlerContext ctx, HttpRequest req,
			MessageEvent e) throws Exception {
		String reqURI = req.getUri();
		log.debug(req.getMethod().getName() + " request uri " + reqURI);

		if (reqURI.endsWith(".js") || reqURI.endsWith(".swf")) {
			handleStaticRequest(req, e, reqURI);
			return;
		}

		// eg:http://localhost/socket.io/1/?t=1332308953338
		if (reqURI.matches("/.*/\\d{1}/([^/]*)?")) {
			handleHandshake(req, e, reqURI);
			return;
		}

		if (currentTransport == null) {
			currentTransport = Transports.getTransportByReq(handler, req);
		}

		if (currentTransport != null) {
			currentTransport.doHandle(ctx, req, e);
			return;
		}

		// if (currentTransport.getId() != Transports.WEBSOCKET.getValue()) {
		sendHttpResponse(ctx, req,
				SocketIOManager.getInitResponse(req, FORBIDDEN));
	}

	private void handleHandshake(HttpRequest req, MessageEvent e, String reqURI) {
		HttpResponse resp = SocketIOManager.getInitResponse(req);
		resp.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");

		final String uID = getUniqueID();
		String contentString = String.format(
				SocketIOManager.getHandshakeResult(), uID);

		QueryStringDecoder queryStringDecoder = new QueryStringDecoder(reqURI);

		String jsonpValue = getParameter(queryStringDecoder, "jsonp");
		// io.j[1]("9135478181958205332:60:60:websocket,flashsocket");
		if (jsonpValue != null) {
			log.debug("request uri with parameter jsonp = " + jsonpValue);
			contentString = "io.j[" + jsonpValue + "]('" + contentString
					+ "');";
			resp.addHeader(CONTENT_TYPE, "application/javascript");
		}

		ChannelBuffer content = ChannelBuffers.copiedBuffer(contentString,
				CharsetUtil.UTF_8);

		resp.addHeader(HttpHeaders.Names.CONNECTION,
				HttpHeaders.Values.KEEP_ALIVE);
		resp.setContent(content);

		e.getChannel().write(resp).addListener(ChannelFutureListener.CLOSE);

		Store store = SocketIOManager.getDefaultStore();
		store.add(uID, BlankIO.getInstance());

		SocketIOManager.schedule(new Runnable() {
			@Override
			public void run() {
				Store store = SocketIOManager.getDefaultStore();
				IOClient client = store.get(uID);
				if (client == null)
					store.remove(uID);
			}
		});
	}

	private static String getParameter(QueryStringDecoder queryStringDecoder,
			String parameterName) {
		if (queryStringDecoder == null || parameterName == null)
			return null;

		List<String> values = queryStringDecoder.getParameters().get(
				parameterName);

		if (values == null || values.isEmpty())
			return null;

		return values.get(0);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-28
	 * 
	 * @param req
	 * @param e
	 * @param reqURI
	 * @throws IOException
	 */
	private void handleStaticRequest(HttpRequest req, MessageEvent e,
			String reqURI) throws IOException {
		String fileName = SocketIOManager.getFileName(req.getUri());
		String contextPath = getClass().getResource("/").toString() + fileName;
		if (contextPath.startsWith("file:/")) {
			contextPath = contextPath.substring(6);
		}
		File file = new File(contextPath);
		RandomAccessFile raf = null;
		try {
			raf = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
			HttpResponse response = new DefaultHttpResponse(HTTP_1_1, NOT_FOUND);
			e.getChannel().write(response)
					.addListener(ChannelFutureListener.CLOSE);
			return;
		}
		long fileLength = raf.length();

		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		setContentLength(response, fileLength);

		Channel ch = e.getChannel();
		ch.write(response);

		ChannelFuture writeFuture;
		if (ch.getPipeline().get(SslHandler.class) != null) {
			// Cannot use zero-copy with HTTPS.
			writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
		} else {
			// No encryption - use zero-copy.
			final FileRegion region = new DefaultFileRegion(raf.getChannel(),
					0, fileLength);
			writeFuture = ch.write(region);
			writeFuture.addListener(new ChannelFutureProgressListener() {
				public void operationComplete(ChannelFuture future) {
					region.releaseExternalResources();
				}

				public void operationProgressed(ChannelFuture future,
						long amount, long current, long total) {
				}
			});
		}

		if (!isKeepAlive(req)) {
			writeFuture.addListener(ChannelFutureListener.CLOSE);
		}
	}

	private void sendHttpResponse(ChannelHandlerContext ctx, HttpRequest req,
			HttpResponse res) {
		if (res.getStatus().getCode() != 200) {
			res.setContent(ChannelBuffers.copiedBuffer(res.getStatus()
					.toString(), CharsetUtil.UTF_8));
			setContentLength(res, res.getContent().readableBytes());
		}

		ChannelFuture f = ctx.getChannel().write(res);
		if (!isKeepAlive(req) || res.getStatus().getCode() != 200) {
			f.addListener(ChannelFutureListener.CLOSE);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		log.debug("exceptionCaught now ...");
		if (this.currentTransport == null) {
			return;
		}

		// 清理资源
		Store store = SocketIOManager.getDefaultStore();
		String sessionId = this.currentTransport.getSessionId();
		IOClient client = store.get(sessionId);
		if (client == null) {
			log.info("client had been removed by session id " + sessionId);
			return;
		}

		if (client instanceof GenericIO) {
			GenericIO genericIO = (GenericIO) client;
			genericIO.scheduleRemoveTask(this.handler);
		}

		log.info("ERROR !");
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}