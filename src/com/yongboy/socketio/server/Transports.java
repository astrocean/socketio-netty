package com.yongboy.socketio.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.transport.FlashSocketTransport;
import com.yongboy.socketio.server.transport.HtmlfileTransport;
import com.yongboy.socketio.server.transport.ITransport;
import com.yongboy.socketio.server.transport.JsonpPollingTransport;
import com.yongboy.socketio.server.transport.WebSocketTransport;
import com.yongboy.socketio.server.transport.XhrPollingTransport;

/**
 * 
 * @author yongboy
 * @time 2012-4-1
 * @version 1.0
 */
public enum Transports {
	XHRPOLLING("xhr-polling", XhrPollingTransport.class), JSONPP0LLING(
			"jsonp-polling", JsonpPollingTransport.class), HTMLFILE("htmlfile",
			HtmlfileTransport.class), WEBSOCKET("websocket",
			WebSocketTransport.class), FLASHSOCKET("flashsocket",
			FlashSocketTransport.class);

	private String value;
	private Class<? extends ITransport> transportClass;

	private Transports(String value, Class<? extends ITransport> transportClass) {
		this.value = value;
		this.transportClass = transportClass;
	}

	public String getValue() {
		return this.value;
	}

	public Class<? extends ITransport> getTransportClass() {
		return this.transportClass;
	}

	public String getUrlPattern() {
		return "/" + getValue() + "/";
	}

	public boolean checkPattern(String uri) {
		if (uri == null)
			return false;

		return uri.contains(getUrlPattern());
	}

	public static ITransport getTransportByReq(IOHandlerAbs handler,
			HttpRequest req) {
		if (req == null || handler == null)
			return null;

		String uri = req.getUri();

		Transports targetTransport = null;
		for (Transports tran : values()) {
			if (!tran.checkPattern(uri)) {
				continue;
			}

			targetTransport = tran;
			break;
		}

		if (targetTransport == null)
			return null;

		Class<? extends ITransport> clazz = targetTransport.getTransportClass();
		Constructor<? extends ITransport> constructor = null;
		try {
			constructor = clazz.getDeclaredConstructor(IOHandlerAbs.class,
					HttpRequest.class);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		if (constructor == null)
			return null;

		try {
			return constructor.newInstance(handler, req);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}

		return null;
	}

	public static Transports getByValue(String value) {
		if (value == null)
			return null;

		for (Transports tran : values()) {
			if (tran.value.equals(value))
				return tran;
		}

		return null;
	}
}
