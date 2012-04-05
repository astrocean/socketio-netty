package com.yongboy.socketio.server;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.ChannelHandlerContext;

import com.yongboy.socketio.server.transport.BlankIO;
import com.yongboy.socketio.server.transport.IOClient;

/**
 * @author yongboy
 * @time 2012-3-29
 * @version 1.0
 */
public class MemoryStore implements Store {

	private static final ConcurrentHashMap<String, IOClient> clients = new ConcurrentHashMap<String, IOClient>();

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.Store#get(java.lang.String)
	 */
	@Override
	public IOClient get(String sessionId) {
		IOClient client = clients.get(sessionId);

		if (client == null)
			return client;

		if (client instanceof BlankIO)
			return null;

		return client;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.Store#remove(java.lang.String)
	 */
	@Override
	public void remove(String sessionId) {
		clients.remove(sessionId);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.Store#add(java.lang.String,
	 * com.yongboy.socketio.transport.IOClient)
	 */
	@Override
	public void add(String sessionId, IOClient client) {
		if (sessionId == null || client == null)
			return;

		clients.put(sessionId, client);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.Store#getClients()
	 */
	@Override
	public Collection<IOClient> getClients() {
		return clients.values();
	}

	@Override
	public boolean checkExist(String sessionId) {
		return clients.containsKey(sessionId);
	}

	@Override
	public IOClient getByCtx(ChannelHandlerContext ctx) {
		if (ctx == null)
			return null;

		for (IOClient client : getClients()) {
			if (ctx == client.getCTX()) {
				return client;
			}
		}

		return null;
	}

	public static void main(String[] args) {
		clients.put("001", BlankIO.getInstance());
		System.out.println(clients.containsKey("001"));
	}
}