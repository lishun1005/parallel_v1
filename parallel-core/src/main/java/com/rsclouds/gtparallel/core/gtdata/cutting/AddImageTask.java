package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rsclouds.gtparallel.core.common.CoreConfig;

public class AddImageTask {
	private ConnectionFactory factory;
	private Connection connection;
	private Channel channel;
	
	public AddImageTask() {
		factory = new ConnectionFactory();
		factory.setHost(CoreConfig.RABBITMQ_HOST);
		factory.setPort(CoreConfig.RABBITMQ_PORT);
		factory.setUsername(CoreConfig.RABBITMQ_USER);
		factory.setPassword("rsclouds@012");
		factory.setVirtualHost(CoreConfig.RABBITMQ_VIRTUAL_HOST);
	}
	
	public boolean initQueue(String queueName) {
		try {
			connection = factory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(queueName, true, false, false, null);
			return true;
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	public  boolean addImageTask(String inputPath, String queueName) {
		if (initQueue(queueName)) {
			File file = new File(inputPath);
			if (file.exists()) {
				try {
					FileReader fileReader = new FileReader(file);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
					String imagePath = null;
					while ((imagePath = bufferedReader.readLine()) != null) {
						channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, imagePath.getBytes());
					}
					return true;
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}finally{
					if (connection != null) {
						try {
							if(channel != null)
								channel.close();
							connection.close();	
						} catch (IOException e) {
							e.printStackTrace();
						} catch (TimeoutException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		return false;
	}
	

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("usage: <inputpath> <rabbitmqname>");
		}else {
			AddImageTask addImageTask = new AddImageTask();
			addImageTask.addImageTask(args[0], args[1]);
		}
		System.exit(0);
	}
}
