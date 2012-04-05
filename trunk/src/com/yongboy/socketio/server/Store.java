package com.yongboy.socketio.server;

import java.util.Collection;

import org.jboss.netty.channel.ChannelHandlerContext;

import com.yongboy.socketio.server.transport.IOClient;

/**
 * 
 * @author nieyong
 * @time 2012-3-29
 * @version 1.0
 */
public interface Store {

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-29
	 * 
	 * @param sessionId
	 */
	void remove(String sessionId);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-29
	 * 
	 * @param sessionId
	 * @param client
	 */
	void add(String sessionId, IOClient client);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-29
	 * 
	 * @return
	 */
	Collection<IOClient> getClients();

	/**
	 * @author nieyong
	 * @time 2012-3-29
	 * 
	 * @param sessionId
	 * @return
	 */
	IOClient get(String sessionId);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-4-3
	 * 
	 * @param sessionId
	 * @return
	 */
	boolean checkExist(String sessionId);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-31
	 * 
	 * @param ctx
	 * @return
	 */
	IOClient getByCtx(ChannelHandlerContext ctx);
}
