package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;

/**
 * 
 * @author nieyong
 * @time 2012-3-27
 * @version 1.0
 */
public interface IOClient {

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 * @param message
	 */
	void send(String message);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 * @param message
	 */
	void sendEncoded(String message);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 * @param beat
	 * @return
	 */
	// boolean heartbeat(int beat);

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 */
	void heartbeat();

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 */
	void disconnect();

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 * @return
	 */
	String getSessionID();

	/**
	 * 
	 * @author nieyong
	 * @time 2012-3-27
	 * 
	 * @return
	 */
	ChannelHandlerContext getCTX();

	/**
	 * return the self's description id ,eg :
	 * xhr-polling/jsonp-polling/websocket
	 * 
	 * @author nieyong
	 * @time 2012-4-1
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 
	 * @author nieyong
	 * @time 2012-4-3
	 * 
	 * @return
	 */
	boolean isOpen();
	
	/**
	 * 
	 * @author nieyong
	 * @time 2012-4-3
	 *
	 * @param open
	 */
	void setOpen(boolean open);
}
