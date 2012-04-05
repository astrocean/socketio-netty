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
import java.util.ArrayList;
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
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;

import com.yongboy.socketio.server.transport.BlankIO;
import com.yongboy.socketio.server.transport.FlashSocketTransport;
import com.yongboy.socketio.server.transport.HtmlfileTransport;
import com.yongboy.socketio.server.transport.IOClient;
import com.yongboy.socketio.server.transport.ITransport;
import com.yongboy.socketio.server.transport.JsonpTransport;
import com.yongboy.socketio.server.transport.PollingTransport;
import com.yongboy.socketio.server.transport.WebSocketTransport;

public class SocketIOTransportAdapter extends SimpleChannelUpstreamHandler {
	private static final Logger log = Logger
			.getLogger(SocketIOTransportAdapter.class);

	private IOHandlerAbs handler;
	private ITransport currentTransport = null;
	private List<ITransport> transportList = null;

	public SocketIOTransportAdapter(IOHandlerAbs handler) {
		super();
		this.handler = handler;

		initTransports();
	}

	private void initTransports() {
		transportList = new ArrayList<ITransport>();
		transportList.add(new HtmlfileTransport(this.handler));
		transportList.add(new PollingTransport(this.handler));
		transportList.add(new WebSocketTransport(this.handler));
		transportList.add(new JsonpTransport(this.handler));
		transportList.add(new FlashSocketTransport(this.handler));
	}

	private ITransport getITransportByUri(String uri) {
		for (ITransport port : transportList) {
			if (port.check(uri))
				return port;
		}

		return null;
	}

	private String getUniqueID() {
		return UUID.randomUUID().toString();
	}

	@Override
	public void channelDisconnected(ChannelHandlerContext ctx,
			org.jboss.netty.channel.ChannelStateEvent e) throws Exception {
		log.debug("channelDisconnected here");

		Store store = SocketIOManager.getDefaultStore();
		IOClient client = store.getByCtx(ctx);
		if (client == null) {
			log.debug("client is null");
			return;
		}

		log.debug("client is not null");
		//
		// // TODO ?
		// this.disconnect(client);
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
			HttpResponse resp = SocketIOManager.getInitResponse(req);
			resp.setStatus(HttpResponseStatus.OK);
			resp.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");

			final String uID = getUniqueID();
			String contentString = String.format(
					SocketIOManager.getHandshakeResult(), uID);

			QueryStringDecoder queryStringDecoder = new QueryStringDecoder(
					reqURI);

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

			return;
		}

		String uri = req.getUri();
		currentTransport = getITransportByUri(uri);
		if (currentTransport != null) {
			currentTransport.doHandle(ctx, req, e);
			return;
		}

		sendHttpResponse(ctx, req,
				SocketIOManager.getInitResponse(req, FORBIDDEN));
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
		e.getCause().printStackTrace();
		e.getChannel().close();
	}
}