package com.rscloud.ipc.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@Component
public class RabbitmqClient {
	private final static Logger logger = LoggerFactory
			.getLogger(RabbitmqClient.class);
	
	private static Object obj=new Object();
	private static String rabbitmqHost;
	@Value("#{applicationProperty[rabbitmqHost]}")
    public void setRabbitmqHost(String str) {
		RabbitmqClient.rabbitmqHost = str;
    }
	
	private static String rabbitmqPort;
	@Value("#{applicationProperty[rabbitmqPort]}")
    public void setRabbitmqPort(String str) {
		RabbitmqClient.rabbitmqPort = str;
    }
	
	private static String rabbitmqUsername;
	@Value("#{applicationProperty[rabbitmqUsername]}")
    public void setRabbitmqUsername(String str) {
		RabbitmqClient.rabbitmqUsername = str;
    }
	
	private static String rabbitmqPassword;
	@Value("#{applicationProperty[rabbitmqPassword]}")
    public void setRabbitmqPassword(String str) {
		RabbitmqClient.rabbitmqPassword = str;
    }
	
	private static String rabbitmqVirtualHost;
	@Value("#{applicationProperty[rabbitmqVirtualHost]}")
    public void setRabbitmqVirtualHost(String str) {
		RabbitmqClient.rabbitmqVirtualHost = str;
    }
	
	private static String queueCut;
	@Value("#{fixparamProperty[queue_cut]}")
    public void setQueueCut(String str) {
		RabbitmqClient.queueCut = str;
    }
	
	private static String queueMosaic;
	@Value("#{fixparamProperty[queue_mosaic]}")
    public void setQueueMosaic(String str) {
		RabbitmqClient.queueMosaic = str;
    }
	
	
	public static Channel channel = null;
	
	public static Channel getChannel() {
		synchronized (obj) {
			if(channel==null){
				try {
					ConnectionFactory factory = new ConnectionFactory();
					factory.setHost(RabbitmqClient.rabbitmqHost);
					factory.setPort(Integer.valueOf(RabbitmqClient.rabbitmqPort.trim()));
					factory.setUsername(RabbitmqClient.rabbitmqUsername);
					factory.setPassword(RabbitmqClient.rabbitmqPassword);
					factory.setVirtualHost(RabbitmqClient.rabbitmqVirtualHost);
					factory.setConnectionTimeout(50000);
					factory.setShutdownTimeout(50000);
					Connection connection = factory.newConnection();
					channel = connection.createChannel();
					//channel.queueDeclare(RabbitmqClient.queueCut, true, false, false, null);
					channel.queueDeclare(RabbitmqClient.queueMosaic, true, false, false, null);
					//logger.info("init queue : {}",RabbitmqClient.queueCut);
					logger.info("init queue : {}",RabbitmqClient.queueMosaic);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return channel;
		}
	}
}
