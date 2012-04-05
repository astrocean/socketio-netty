package com.yongboy.socketio.server.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.yongboy.socketio.server.SocketIOManager;

/**
 * 
 * @author nieyong
 * @time 2012-4-1
 * @version 1.0
 */
abstract class EventClientIO implements IOClient {
	private Set<String> events = null;
	public Map<String, Object> attr = null;

	public EventClientIO() {
		events = new HashSet<String>();
		attr = new HashMap<String, Object>();
	}

	public void addEvent(String event) {
		if (SocketIOManager.fobbiddenEvents.contains(event))
			return;

		events.add(event);
	}

	public void removeEvent(String event) {
		events.remove(event);
	}

	public Set<String> getEvents() {
		return Collections.unmodifiableSet(events);
	}

	public boolean containsEvent(String event) {
		return events.contains(event);
	}

	public void clearEvents() {
		events.clear();
	}
}