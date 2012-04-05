package com.yongboy.socketio.server.transport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Store;

/**
 * 
 * @author nieyong
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
	protected final BlockingQueue<String> queue;

	protected ScheduledFuture<?> scheduledFuture;

	public GenericIO(ChannelHandlerContext ctx, HttpRequest req, String uID) {
		super();
		this.ctx = ctx;
		this.req = req;
		this.uID = uID;
		this.open = true;
		
		queue = new LinkedBlockingQueue<String>();
	}

	public void reconnect(ChannelHandlerContext ctx, HttpRequest req) {
		this.ctx = ctx;
		this.req = req;

		this.open = true;
	}

	public void send(String message) {
		sendEncoded(message);
	}

	public void heartbeat() {
		prepareHearbeat();

		// add new task
		// 定时设置client为不可用
		scheduleClearTask();

		sendEncoded("2::");
	}

	protected void scheduleClearTask() {
		scheduledFuture = SocketIOManager.scheduleClearTask(new ClearTask(
				getSessionID()));
	}

	protected void prepareHearbeat() {
		if (this.beat > 0) {
			this.beat++;
		}

		// 清除已有定时器
		log.debug("scheduledFuture is null ? " + (scheduledFuture == null));
		if (scheduledFuture != null) {
			if (!(scheduledFuture.isCancelled() || scheduledFuture.isDone())){
				log.debug("going to cancel the task ~");
				scheduledFuture.cancel(true);
			}else{
				log.debug("scheduledFuture had been canceled");
			}
		}
	}

	/**
	 * 
	 * @author nieyong
	 * @time 2012-4-4
	 * @version 1.0
	 */
	protected static class ClearTask implements Runnable {
		private String sessionId;
		private boolean clearSession = false;

		public ClearTask(String sessionId) {
			this.sessionId = sessionId;
		}

		public ClearTask(String sessionId, boolean clearSession) {
			this.sessionId = sessionId;
			this.clearSession = clearSession;
		}

		@Override
		public void run() {
			log.debug("entry ClearTask run method clearSession is "
					+ clearSession + " and sessionId is " + sessionId);
			Store store = SocketIOManager.getDefaultStore();
			IOClient client = store.get(sessionId);
			if (client == null) {
				log.debug("the client is null");
				return;
			}

			if (!clearSession && client.isOpen()) {
				client.setOpen(false);
				// maybe you need to save it into database
				// some update method here
			}

			if (!clearSession) {
				log.debug("add new task ~");
				SocketIOManager
						.scheduleClearTask(new ClearTask(sessionId, true));
				return;
			}

			// start new task to clear the client object
			// 若被其它线程激活，则意味着当前client为有效状态
			if (client.isOpen()) {
				log.debug("the client's open is " + client.isOpen());
				return;
			}

			log.debug("now remove the clients from store with sessionid " + sessionId);
			store.remove(sessionId);
		}
	}

	// public boolean heartbeat(int beat) {
	// if (!this.open)
	// return false;
	//
	// int lastBeat = beat - 1;
	// if (this.beat == 0 || this.beat > beat) {
	// this.beat = beat;
	// } else if (this.beat < lastBeat) {
	// // we're 2 beats behind..
	// return false;
	// }
	// return true;
	// }

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
	 * @author nieyong
	 * @time 2012-3-23
	 * 
	 */
	public void prepare() {
		// TO DO NOTHING ...
	}

	/**
	 * 1:: 在初次连接时调用
	 * 
	 * @author nieyong
	 * @time 2012-3-23
	 * 
	 */
	public void connect(String info) {
		sendEncoded("1::");
	}

	/**
	 * 0 :: 连接时
	 * 
	 * @author nieyong
	 * @time 2012-3-23
	 * 
	 * @param info
	 */
	public void disconnect(String info) {
		sendEncoded("0::");
	}

	/**
	 * 定义POST方式定时心跳检测，输出值
	 * 
	 * @author nieyong
	 * @time 2012-3-23
	 * 
	 */
	// public void postHeartbeat() {
	// if (this.beat > 0) {
	// this.beat++;
	// }
	//
	// sendEncoded("1");
	// }

	@Override
	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void setOpen(boolean open) {
		this.open = open;
	}
}