package com.rsclouds.common.utils;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ShellExecutor {
	private static Logger logger = LoggerFactory.getLogger(ShellExecutor.class);

	private Connection conn;
	private String ip;
	
	private int port;
	private String user;
	
	private String passsword;

	private static final int TIME_OUT = 1000 * 60 * 60 * 24;
	public ShellExecutor(String ip, int port, String user, String passsword) {
		this.ip = ip;
		this.user = user;
		this.passsword = passsword;
		this.port = port == 0 ? 22 : port;
	}
	private boolean login() throws IOException {
		conn = new Connection(ip, port);
		conn.connect();
		return conn.authenticateWithPassword(user, passsword);
	}
	/**
	 * @Description:执行远程脚本 --同步
	 * @author lishun
	 * @date 2018/1/30
	 * @param [shell]
	 * @return 0 成功 , !0 失败
	 */
	public Integer execRemoteSynchronization(String shell) throws Exception {
		Integer ret = null;
		try {
			if (login()) {
				Session session = conn.openSession(); // Open a new {@link Session} on this connection
				session.execCommand(shell); // Execute a command on the remote machine.
				session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
				ret = session.getExitStatus();
				if (session != null) {
					session.close();
				}
			} else {
				throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
			}
		} finally {
			if (conn != null) {
				conn.close();
			}
			if (ret == null) {
				ret = -1;
			}
		}
		return ret;
	}
	/**
	 * 执行本地shell命令
	 * @param shell
	 * @return 0 成功, !0 失败
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static int execLocal(String shell) throws IOException, InterruptedException{
		int status = -1;
		Process process= null;
		try {
			Runtime rt = Runtime.getRuntime();
			process = rt.exec(shell);
			status = process.waitFor();
		}finally{
			if (process != null) {
				process.destroy();
				process = null;
			}
		}
		return status;
	}
	/**
	 * @Description:执行远程脚本 -- 异步:执行shell不需要获取返回信息
	 * @author lishun
	 * @date 2018/1/30
	 * @param [shell]
	 * @return 0 成功 , !0 失败
	 */
	public void execRemoteAsynchronous(String shell) throws Exception {
		try {
			if (login()) {
				Session session = conn.openSession(); // Open a new {@link Session} on this connection
				session.execCommand(shell); // Execute a command on the remote machine.
				session.waitForCondition(ChannelCondition.EXIT_STATUS, 1000);
				if (session != null) {
					session.close();
				}
			} else {
				throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
			}
		} finally {
			if (conn != null) {
				conn.close();
			}
		}
	}
	public static void main(String args[]) throws Exception {
		/*ShellExecutor exe = new ShellExecutor("10.0.78.11", 22, "root","rsclouds@456");// 执行myTest.sh 参数为java Know dummy
		System.out.println("sttat");
		//String shell = "/opt/test.sh";
		String shell = "hadoop jar /home/webserver/parallel-core-0.0.1.jar com.rsclouds.gtparallel.core.App ONEMAP 55c4fab3d4b94b55922069e2b388fcdf gtdata:///users//zmp/南昌市.tiff /map/autotest/7/Layers/_alllayers 1  -watermark false -minLayers 0 -zero_percentage 0.01 -save_storage true -bcover true";
		exe.execRemoteAsynchronous(shell);*/
	}
}