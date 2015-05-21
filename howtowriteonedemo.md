**怎么写一个Demo**

# Introduction #
  1. 继承\*com.yongboy.socketio.server.IOHandlerAbs\*抽象类
  1. 重写OnConnect，OnDisconnect，OnMessage，OnShutdown方法
  1. 准备好前端代码即可

# Details #
示范代码：
```
	public static void main(String[] args) {
		MainServer chatServer = new MainServer(new DemoChatHandler(), 80);
		chatServer.start();
	}

public class DemoChatHandler extends IOHandlerAbs {
	private Logger log = Logger.getLogger(this.getClass());
	private ConcurrentMap<String, String> nicknames = new ConcurrentHashMap<String, String>();

	@Override
	public void OnConnect(IOClient client) {
		log.debug("A user connected :: " + client.getSessionID());
	}

	@Override
	public void OnDisconnect(IOClient client) {
		log.debug("A user disconnected :: " + client.getSessionID()
				+ " :: hope it was fun");

		GenericIO genericIO = (GenericIO) client;
		Object nickNameObj = genericIO.attr.get("nickName");

		if (nickNameObj == null)
			return;

		String nickName = nickNameObj.toString();
		nicknames.remove(nickName);
		emit("announcement", nickName + "  disconnected");

		emit("nicknames", nicknames);
	}

	@Override
	public void OnMessage(IOClient client, String oriMessage) {
		log.debug("Got a message :: " + oriMessage
				+ " :: echoing it back to :: " + client.getSessionID());
		String jsonString = oriMessage.substring(oriMessage.indexOf('{'));
		jsonString = jsonString.replaceAll("\\\\", "");
		log.debug("jsonString " + jsonString);
		JSONObject jsonObject = JSON.parseObject(jsonString);
		String eventName = jsonObject.get("name").toString();

		if (eventName.equals("nickname")) {
			JSONArray argsArray = jsonObject.getJSONArray("args");
			String nickName = argsArray.getString(0);
			if (nicknames.containsKey(nickName)) {
				handleAckNoticName(client, oriMessage, true);

				return;
			}

			handleAckNoticName(client, oriMessage, false);
			nicknames.put(nickName, nickName);

			GenericIO genericIO = (GenericIO) client;
			genericIO.attr.put("nickName", nickName);

			emit("announcement", nickName + " connected");
			emit("nicknames", nicknames);
			return;
		}

		if (eventName.equals("user message")) {
			GenericIO genericIO = (GenericIO) client;
			String nickName = genericIO.attr.get("nickName").toString();

			JSONArray argsArray = jsonObject.getJSONArray("args");
			String message = argsArray.getString(0);

			emit(client, eventName, nickName, message);
		}
	}

	/**
	 * 处理用户名的通知
	 * 
	 * @author yongboy
	 * @time 2012-3-31
	 * 
	 * @param client
	 * @param oriMessage
	 * @param obj
	 */
	private void handleAckNoticName(IOClient client, String oriMessage,
			Object obj) {
		// 处理带有 消息id+ 的情况
		boolean aplus = oriMessage.matches("\\d:\\d{1,}\\+:.*:.*?");
		if (aplus) {
			String aPlusStr = oriMessage.substring(2,
					oriMessage.indexOf('+') + 1);
			// 通知ioclient发送此消息
			ackNotify(client, aPlusStr, obj);
		}
	}

	@Override
	public void OnShutdown() {
		log.debug("shutdown now ~~~");
	}

	private void emit(String eventName, Map<String, String> nicknames) {
		String content = String.format("{\"name\":\"%s\",\"args\":[%s]}",
				eventName, JSON.toJSONString(nicknames));
		super.broadcast(content);
	}

	private void emit(String eventName, String message) {
		String content = String.format("{\"name\":\"%s\",\"args\":[\"%s\"]}",
				eventName, message);
		super.broadcast(content);
	}

	private void emit(IOClient client, String eventName, String message,
			String message2) {
		String content = String.format(
				"{\"name\":\"%s\",\"args\":[\"%s\",\"%s\"]}", eventName,
				message, message2);
		super.broadcast(client, content);
	}
}
```

具体请参考/socketio-netty/test/com/yongboy/socketio/test/ChatServer.java