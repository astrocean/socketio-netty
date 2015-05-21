#如何添加namespace/endpoint

# Introduction #
添加多个命名空间支持
MainServer方法提供addNamespace方法，用以添加命名空间和namespace的具体执行类

# Details #
```
	public static void main(String[] args) {
		int port = 9000;

		/**
		 * 兼容cloudfoudry平台
		 */
		String envPort = System.getenv("VCAP_APP_PORT");
		if (envPort != null && envPort.trim().length() > 0) {
			port = Integer.parseInt(envPort.trim());
		}

		MainServer mainServer = new MainServer(port);
		mainServer.addNamespace("/whiteboard", new WhiteboardHandler());
		mainServer.addNamespace("/chat", new DemoChatHandler());
		mainServer.start();
	}
```