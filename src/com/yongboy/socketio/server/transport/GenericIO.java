package com.yongboy.socketio.server.transport;

import java.util.concurrent.TimeUnit;

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

	/**
	 * 命名空间
	 */
	private String namespace = "";

	public GenericIO(ChannelHandlerContext ctx, HttpRequest req, String uID,
			String namespace) {
		this(ctx, req, uID);

		this.namespace = namespace;
	}

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
		if (!message.matches("\\d:.*?")) {
			message = "5::" + getNamespace() + ":" + message;
		}

		sendEncoded(message);
	}

	/**
	 * 仅作为示范，会被子类重写
	 * 
	 * @author yongboy
	 * @time 2012-4-6
	 * 
	 */
	public void heartbeat(final IOHandler handler) {
		// 以下为示范代码
		/*
		 * prepareHearbeat();
		 * 
		 * // add new task // 定时设置client为不可用 scheduleClearTask(handler);
		 * 
		 * sendEncoded("2::");
		 */
	}

	protected void scheduleClearTask(final IOHandler handler) {
		scheduledFuture = SocketIOManager.scheduleClearTask(new ClearTaskSpeed(
				getSessionID(), handler),
				SocketIOManager.option.heartbeat_timeout, TimeUnit.SECONDS);
	}

	public void scheduleRemoveTask(final IOHandler handler) {
		scheduledFuture = SocketIOManager.scheduleClearTask(new ClearTaskSpeed(
				getSessionID(), handler), 1L, TimeUnit.MILLISECONDS);
	}

	protected void prepareHeartbeat() {
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
		if (info != null && info.length() > 0)
			sendEncoded(info);
		else
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.yongboy.socketio.server.transport.IOClient#getNamespace()
	 */
	@Override
	public String getNamespace() {
		return this.namespace;
	}

	/**
	 * @param namespace
	 *            the namespace to set
	 */
	public void setNamespace(String namespace) {
		if (namespace != null)
			this.namespace = namespace;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getSessionID().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;

		if (this.getClass() != obj.getClass()) {
			return false;
		}

		IOClient objClient = (IOClient) obj;

		return this.getId().equals(objClient.getId())
				&& this.getSessionID().equals(objClient.getSessionID());
	}
}