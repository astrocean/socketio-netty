package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;

import com.yongboy.socketio.server.IOHandler;

/**
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public interface IOClient {

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 * @param message
	 */
	void send(String message);

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 * @param message
	 */
	void sendEncoded(String message);

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 * @param beat
	 * @return
	 */

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 */
	void heartbeat(final IOHandler ioHandler);

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 */
	void disconnect();

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 * @return
	 */
	String getSessionID();

	/**
	 * 
	 * @author yongboy
	 * @time 2012-3-27
	 * 
	 * @return
	 */
	ChannelHandlerContext getCTX();

	/**
	 * return the self's description id ,eg :
	 * xhr-polling/jsonp-polling/websocket
	 * 
	 * @author yongboy
	 * @time 2012-4-1
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 
	 * @author yongboy
	 * @time 2012-4-3
	 * 
	 * @return
	 */
	boolean isOpen();

	/**
	 * 
	 * @author yongboy
	 * @time 2012-4-3
	 * 
	 * @param open
	 */
	void setOpen(boolean open);
}
