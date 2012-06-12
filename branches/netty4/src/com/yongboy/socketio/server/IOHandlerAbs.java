package com.yongboy.socketio.server;

import java.util.Collection;

import org.apache.log4j.Logger;

import com.alibaba.fastjson.JSON;
import com.yongboy.socketio.server.transport.IOClient;

/**
 * 
 * @author yongboy
 * @time 2012-3-30
 * @version 1.0
 */
public abstract class IOHandlerAbs implements IOHandler {
	private static final Logger log = Logger.getLogger(IOHandlerAbs.class);

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-31
	 * 
	 * @return
	 */
	protected Collection<IOClient> getClients() {
		Store store = SocketIOManager.getDefaultStore();

		return store.getClients();
	}

	/**
	 * 广播方式发送到各个连接断点
	 * 
	 * @param message
	 */
	protected void broadcast(String message) {
		broadcast(null, message);
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-31
	 * 
	 * @param current
	 *            若不为null，则广播剩余节点，否则广播到所有
	 * @param message
	 */
	protected void broadcast(IOClient current, String message) {
		for (IOClient client : getClients()) {
			if (current != null
					&& current.getSessionID() == client.getSessionID())
				continue;

			client.send(message);
		}
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-31
	 * 
	 */
	protected void ackNotify(IOClient client, String messageIdPlusStr,
			Object obj) {
		StringBuilder builder = new StringBuilder("6::");
		builder.append(client.getNamespace()).append(":");

		String formateJson = JSON.toJSONString(obj);
		if (formateJson.startsWith("[") && formateJson.endsWith("]")) {
			formateJson = formateJson.substring(1, formateJson.length() - 1);
		}

		builder.append(messageIdPlusStr).append("[").append(formateJson)
				.append("]");

		log.debug("ack message " + builder);
		client.send(builder.toString());
	}
}