package com.yongboy.socketio.server.transport;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;

import org.apache.log4j.Logger;

import com.yongboy.socketio.server.IOHandler;
import com.yongboy.socketio.server.SocketIOManager;
import com.yongboy.socketio.server.Store;

/**
 * 
 * @author yongboy
 * @time 2012-4-1
 * @version 1.0
 */
abstract class EventClientIO implements IOClient {
	private static final Logger log = Logger.getLogger(EventClientIO.class);

	public Map<String, Object> attr = null;
	protected final BlockingQueue<String> queue;
	protected ScheduledFuture<?> scheduledFuture;

	public EventClientIO() {
		attr = new HashMap<String, Object>();
		queue = new LinkedBlockingQueue<String>();
	}

	/**
	 * 
	 * @author yongboy
	 * @time 2012-4-4
	 * @version 1.0
	 */
	protected static class ClearTask implements Runnable {
		private String sessionId;
		private boolean clearSession = false;
		private IOHandler handler = null;

		public ClearTask(String sessionId, final IOHandler handler) {
			this.sessionId = sessionId;
			this.handler = handler;
		}

		public ClearTask(String sessionId, final IOHandler handler,
				boolean clearSession) {
			this(sessionId, handler);

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
				SocketIOManager.scheduleClearTask(new ClearTask(sessionId,
						handler, true));
				return;
			}

			// start new task to clear the client object
			// 若被其它线程激活，则意味着当前client为有效状态
			if (client.isOpen()) {
				log.debug("the client's open is " + client.isOpen());
				return;
			}

			log.debug("now remove the clients from store with sessionid "
					+ sessionId);

			if (handler != null) {
				handler.OnDisconnect(client);
			} else {
				log.debug("ioHandler is null");
			}

			client.disconnect();
			store.remove(sessionId);
		}
	}
}