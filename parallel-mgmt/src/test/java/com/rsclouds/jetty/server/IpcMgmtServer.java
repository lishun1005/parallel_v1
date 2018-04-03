package com.rsclouds.jetty.server;


/**
 * 
 * Description:使用Jetty运行调试Web应用, 在Console输入回车快速重新加载应用
 *
 * @version v1.0
 *
 */
public class IpcMgmtServer {
	public static final int PORT = 8090;
	public static final String CONTEXT = "/";
	public static final String[] TLD_JAR_NAMES = new String[] { "spring-webmvc" };
	
	public static final String ACTIVE_PROFILE = "spring.profiles.active";
	public static final String PRODUCTION = "production";
	public static final String DEVELOPMENT = "dev";

	public static void main(String[] args) throws Exception {
		/*System.setProperty(ACTIVE_PROFILE, DEVELOPMENT);// 设定Spring的profile
		Server server = JettyFactory.createServerInSource(PORT, CONTEXT);
		JettyFactory.setTldJarNames(server, TLD_JAR_NAMES);
		try {
			server.start();
			System.out.println("[INFO] Server running at http://127.0.0.1:" + PORT + CONTEXT);
			System.out.println("[HINT] Hit Enter to reload the application quickly");
			while (true) {
				char c = (char) System.in.read();
				if (c == '\n') {
					JettyFactory.reloadContext(server);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}*/
	}
}