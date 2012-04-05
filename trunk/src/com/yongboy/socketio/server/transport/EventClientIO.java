package com.yongboy.socketio.server.transport;

import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author yongboy
 * @time 2012-4-1
 * @version 1.0
 */
abstract class EventClientIO implements IOClient {
	public Map<String, Object> attr = null;

	public EventClientIO() {
		attr = new HashMap<String, Object>();
	}
}