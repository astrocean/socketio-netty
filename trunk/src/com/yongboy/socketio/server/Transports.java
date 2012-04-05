package com.yongboy.socketio.server;

/**
 * 
 * @author yongboy
 * @time 2012-4-1
 * @version 1.0
 */
public enum Transports {
	XHRPOLLING("xhr-polling"), JSONPP0LLING("jsonp-polling"), HTMLFILE(
			"htmlfile"), WEBSOCKET("websocket"), FLASHSOCKET("flashsocket");

	private String value;

	private Transports(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

	public String getUrlPattern() {
		return "/" + getValue() + "/";
	}

	public boolean checkPattern(String uri) {
		if (uri == null)
			return false;

		return uri.contains(getUrlPattern());
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
