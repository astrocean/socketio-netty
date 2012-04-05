package com.yongboy.socketio.server;

import java.util.Arrays;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.HttpVersion;

/**
 * 
 * @author yongboy
 * @time 2012-3-28
 * @version 1.0
 */
public class SocketIOManager {
	public static Option option = new Option();

	private static final ScheduledExecutorService scheduledExecutorService = Executors
			.newScheduledThreadPool(1);

	public static final Set<String> fobbiddenEvents = new HashSet<String>(
			Arrays.asList("message,connect,disconnect,open,close,error,retry,reconnect"
					.split(",")));

	private static Store store = new MemoryStore();

	public static final class Option {
		public boolean heartbeat = true;
		public int heartbeat_timeout = 60;
		public int close_timeout = 60;
		public int heartbeat_interval = 25;
		public boolean flash_policy_server = true;
		public int flash_policy_port = 10843;
		public String transports = "websocket,jsonp-polling,xhr-polling";

		{
			ResourceBundle bundle = ResourceBundle.getBundle("socketio");

			heartbeat = bundle.getString("heartbeat").equals("true");
			heartbeat_timeout = Integer.parseInt(bundle
					.getString("heartbeat_timeout"));
			close_timeout = Integer.parseInt(bundle.getString("close_timeout"));
			heartbeat_interval = Integer.parseInt(bundle
					.getString("heartbeat_interval"));
			flash_policy_server = bundle.getString("flash_policy_server")
					.equals("true");
			flash_policy_port = Integer.parseInt(bundle
					.getString("flash_policy_port"));
			transports = bundle.getString("transports");
		}
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-29
	 * 
	 * @return
	 */
	public static Store getDefaultStore() {
		return store;
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-28
	 * 
	 * @param runnable
	 */
	public static void schedule(Runnable runnable) {
		scheduledExecutorService.schedule(runnable, option.heartbeat_interval,
				TimeUnit.SECONDS);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-28
	 * 
	 * @return
	 */
	public static String getHandshakeResult() {
		return "%s:"
				+ (option.heartbeat ? Integer
						.toString(option.heartbeat_timeout) : "") + ":"
				+ option.close_timeout + ":" + option.transports;
	}

	/**
	 * 统一控制是否跨域请求等
	 * 
	 * @author yongboy
	 * @time 2012-3-28
	 * 
	 * @param req
	 * @return
	 */
	public static HttpResponse getInitResponse(HttpRequest req) {
		return getInitResponse(req, HttpResponseStatus.OK);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-28
	 * 
	 * @param req
	 * @param status
	 * @return
	 */
	public static HttpResponse getInitResponse(HttpRequest req,
			HttpResponseStatus status) {
		HttpResponse resp = new DefaultHttpResponse(HttpVersion.HTTP_1_1,
				status);

		if (req != null && req.getHeader("Origin") != null) {
			resp.addHeader("Access-Control-Allow-Origin",
					req.getHeader("Origin"));
			resp.addHeader("Access-Control-Allow-Credentials", "true");
		}

		return resp;
	}

	/**
	 * @author yongboy
	 * @time 2012-4-3
	 * 
	 * @param runnable
	 * @return
	 */
	public static ScheduledFuture<?> scheduleClearTask(Runnable runnable) {
		return scheduledExecutorService
				.schedule(runnable, option.heartbeat_timeout, TimeUnit.SECONDS);
	}

	/**
	 * 得到文件名
	 * 
	 * @author yongboy
	 * @time 2012-4-5
	 * 
	 * @param filename
	 * @return
	 */
	public static String getFileName(String filename) {
		if (filename == null) {
			return null;
		}
		int index = indexOfLastSeparator(filename);
		return filename.substring(index + 1);
	}

	private static int indexOfLastSeparator(String filename) {
		if (filename == null) {
			return -1;
		}
		int lastUnixPos = filename.lastIndexOf('/');
		int lastWindowsPos = filename.lastIndexOf('\\');
		return Math.max(lastUnixPos, lastWindowsPos);
	}
}