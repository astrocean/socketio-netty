package com.yongboy.socketio.server.transport;

import org.jboss.netty.channel.ChannelHandlerContext;

import com.yongboy.socketio.server.IOHandler;

/**
 * @author yongboy
 * @time 2012-4-3
 * @version 1.0
 */
public class BlankIO implements IOClient {

	private static BlankIO blankIO = null;

	public static BlankIO getInstance() {
		if (blankIO == null)
			blankIO = new BlankIO();

		return blankIO;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.yongboy.socketio.server.transport.IOClient#send(java.lang.String)
	 */
	@Override
	public void send(String message) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.yongboy.socketio.server.transport.IOClient#sendEncoded(java.lang.
	 * String)
	 */
	@Override
	public void sendEncoded(String message) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#heartbeat()
	 */
	@Override
	public void heartbeat(final IOHandler ioHandler) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#disconnect()
	 */
	@Override
	public void disconnect() {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getSessionID()
	 */
	@Override
	public String getSessionID() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getCTX()
	 */
	@Override
	public ChannelHandlerContext getCTX() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getId()
	 */
	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#isOpen()
	 */
	@Override
	public boolean isOpen() {
		// TODO Auto-generated method stub
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#setOpen(boolean)
	 */
	@Override
	public void setOpen(boolean open) {
	}

	/* (non-Javadoc)
	 * @see com.yongboy.socketio.server.transport.IOClient#getNamespace()
	 */
	@Override
	public String getNamespace() {
		return null;
	}
}