package com.rsclouds.gtparallel.core.test2;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import com.rabbitmq.client.*;
import com.rsclouds.gtparallel.core.common.CoreConfig;

public class RabbitmqTest {
	private final static String QUEUE_NAME = CoreConfig.RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME;

	private static void doWork(String task) {
		for (char ch : task.toCharArray()) {
			if (ch == '.') {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException _ignored) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}

	public static void main(String[] argv) throws java.io.IOException,
			java.lang.InterruptedException, TimeoutException {

		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(CoreConfig.RABBITMQ_HOST);
		factory.setPort(CoreConfig.RABBITMQ_PORT);
		factory.setUsername(CoreConfig.RABBITMQ_USER);
		factory.setPassword("rsclouds@012");
		factory.setVirtualHost(CoreConfig.RABBITMQ_VIRTUAL_HOST);
		Connection connection = factory.newConnection();
		Channel channel = connection.createChannel();

		channel.queueDeclare(QUEUE_NAME, false, false, false, null);
		System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
		 QueueingConsumer consumer = new QueueingConsumer(channel); 
//		Consumer consumer = new DefaultConsumer(channel) {
//			@Override
//			public void handleDelivery(String consumerTag, Envelope envelope,
//					AMQP.BasicProperties properties, byte[] body)
//					throws IOException {
//				String message = new String(body, "UTF-8");
//				System.out.println(" [x] Received '" + message + "'");
//			}
//		};
		channel.basicConsume(QUEUE_NAME, true, consumer);
		while (true)  
        {  
            QueueingConsumer.Delivery delivery = consumer.nextDelivery();  
            String message = new String(delivery.getBody());  
  
            System.out.println("[x] Received '" + message + "'");  
            doWork(message);  
            System.out.println("[x] Done");  
  
        } 
	}
}
