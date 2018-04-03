package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.Session;
import ch.ethz.ssh2.StreamGobbler;


public class RemoteShell {
	 /** *//**  */
   private Connection conn;
   /** *//** 远程机器IP */
   private String    ip;
   private int port;
   /** *//** 用户名 */
   private String    usr;
   /** *//** 密码 */
   private String    psword;
   private String    psword_default = "rscloud@456";
   private String    charset = Charset.defaultCharset().toString();

   private static final int TIME_OUT = 1000 * 60 * 60 * 24;


   /** *//**
   * 构造函数
   * @param ip
   * @param usr
   * @param ps
   */
   public RemoteShell(String ip,int port, String usr, String ps) {
       this.ip = ip;
       this.usr = usr;
       this.psword = ps;
       this.port = port == 0 ? 22:port;
   }

   /** *//**
   * 登录
   *
   * @return
   * @throws IOException
   */
   private boolean login() throws IOException {
       conn = new Connection(ip, port);
       conn.connect();
       if (conn.authenticateWithPassword(usr, psword)) {
    	   System.out.println("login sucessful psword:" + psword);
    	   return true;
       }else if(conn.authenticateWithPassword(usr, psword_default)) {
    	   System.out.println("login sucessful psword_default:" + psword_default);
    	   return true;
       }
       return false;
   }

   
   /** *//** 

    * @param in 

    * @param charset 

    * @return 

    * @throws IOException 

    * @throws UnsupportedEncodingException 

    */ 

   private String processStream(InputStream in, String charset) throws Exception { 

       byte[] buf = new byte[1024]; 

       StringBuilder sb = new StringBuilder(); 

       while (in.read(buf) != -1) { 

           sb.append(new String(buf, charset)); 

       } 

       return sb.toString(); 

   }
   
   
   /** *//**
    * 执行脚本
    *
    * @param cmds
    * @return
    * @throws Exception
    */
    public Integer exec(String cmds) throws Exception {
        Integer ret = null;
        try {
            if (login()) {
                Session session = conn.openSession();
                session.execCommand(cmds);
                
                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT);
                StreamGobbler stdOut = new StreamGobbler(session.getStdout()); 

                String outStr = processStream(stdOut, charset); 

   
                StreamGobbler stdErr = new StreamGobbler(session.getStderr()); 
                String outErr = processStream(stdErr, charset);               

                session.waitForCondition(ChannelCondition.EXIT_STATUS, TIME_OUT); 

                

                System.out.println("outStr=" + outStr); 

                System.out.println("outErr=" + outErr);
                ret = session.getExitStatus();
                if(session != null){
                	System.out.println("close session");
           		 	session.close();
           	 	}
            } else {
                throw new Exception("登录远程机器失败" + ip); // 自定义异常类 实现略
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
            if(ret == null){
           	 ret = -1;
            }
        }
        return ret;
    }
    

//   /** *//**
//   * @param in
//   * @param charset
//   * @return
//   * @throws IOException
//   * @throws UnsupportedEncodingException
//   */
//   private String processStream(InputStream in, String charset) throws Exception {
//       byte[] buf = new byte[1024];
//       StringBuilder sb = new StringBuilder();
//       while (in.read(buf) != -1) {
//           sb.append(new String(buf, charset));
//       }
//       return sb.toString();
//   }

   public static void main(String args[]) throws Exception {
       RemoteShell exe = new RemoteShell("10.0.79.2",22, "root", "123456");
       // 执行myTest.sh 参数为java Know dummy 
       int status_1 = exe.exec("scp /home/yarn/123456_pl/image.geojson root@10.0.79.4:/home/yarn/b0ea2a08-6c2d-4881-8963-017310dc9d8e.geojson");
       System.out.println(status_1);
       int status = exe.exec("HADOOP_CLASSPATH=/usr/lib/hbase/lib/hbase-protocol.jar hadoop jar /home/webserver/parallel-core-0.0.1-SNAPSHOT.jar com.rsclouds.gtparallel.core.App GENERATE_MAP_CONF hdfs://node03.rsclouds.cn:8020/temp/8570919576174_testUser_fe35661e-881e-4afb-959e-efb2d6baba51/test.tif /download/cutting-out/test-4/Layers /download/cutting-out/test-4/Layers/_alllayers 1");
       System.out.println(status);
       
   }
}