package com.yongboy.socketio.server.transport;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.IOHandler;
import com.yongboy.socketio.server.SocketIOManager;

/**
 * 
 * @author yongboy
 * @time 2012-3-27
 * @version 1.0
 */
public abstract class GenericIO extends EventClientIO implements IOClient {
	private static final Logger log = Logger.getLogger(GenericIO.class);

	protected ChannelHandlerContext ctx;
	protected int beat;
	protected String uID;
	protected boolean open = false;
	protected HttpRequest req;

	public GenericIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super();
		this.ctx = ctx;
		this.req = req;
		this.uID = uID;
		this.open = true;
	}

	public void reconnect(ChannelHandlerContext ctx, HttpRequest req) {
		this.ctx = ctx;
		this.req = req;

		this.open = true;
	}

	public void send(String message) {
		sendEncoded(message);
	}

	/**
	 * 仅作为示范，会被子类重写
	 * 
	 * @author nieyong
	 * @time 2012-4-6
	 * 
	 */
	public void heartbeat(final IOHandler handler) {
		prepareHearbeat();

		// add new task
		// 定时设置client为不可用
		scheduleClearTask(handler);

		sendEncoded("2::");
	}

	protected void scheduleClearTask(final IOHandler handler) {
		scheduledFuture = SocketIOManager.scheduleClearTask(new ClearTask(
				getSessionID(), handler));
	}

	protected void prepareHearbeat() {
		if (this.beat > 0) {
			this.beat++;
		}

		// 清除已有定时器
		log.debug("scheduledFuture is null ? " + (scheduledFuture == null));
		if (scheduledFuture != null) {
			if (!(scheduledFuture.isCancelled() || scheduledFuture.isDone())) {
				log.debug("going to cancel the task ~");
				scheduledFuture.cancel(true);
			} else {
				log.debug("scheduledFuture had been canceled");
			}
		}
	}

	public ChannelHandlerContext getCTX() {
		return this.ctx;
	}

	public String getSessionID() {
		return this.uID;
	}

	public void disconnect() {
		Channel chan = ctx.getChannel();
		if (chan.isOpen()) {
			chan.close();
		}
		this.open = false;
	}

	public abstract void sendEncoded(String message);

	/**
	 * 预先执行一些操作
	 * 
	 * @author yongboy
	 * @time 2012-3-23
	 * 
	 */
	public void prepare() {
		// TO DO NOTHING ...
	}

	/**
	 * 1:: 在初次连接时调用
	 * 
	 * @author yongboy
	 * @time 2012-3-23
	 * 
	 */
	public void connect(String info) {
		sendEncoded("1::");
	}

	/**
	 * 0 :: 连接时
	 * 
	 * @author yongboy
	 * @time 2012-3-23
	 * 
	 * @param info
	 */
	public void disconnect(String info) {
		sendEncoded("0::");
	}

	@Override
	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void setOpen(boolean open) {
		this.open = open;
	}
}