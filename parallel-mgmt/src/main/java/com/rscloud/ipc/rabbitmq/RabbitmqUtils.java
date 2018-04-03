package com.rscloud.ipc.rabbitmq;

import java.io.IOException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;

public class RabbitmqUtils {
	public static void removeQueueMsg(final String queueName,final String removeMsg){
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Channel channel = RabbitmqClient.getChannel();
					QueueingConsumer consumer = new QueueingConsumer(channel);
					try {
						channel.basicConsume(queueName,false, consumer);
					} catch (IOException e) {
						e.printStackTrace();
					}
					Delivery delivery=null;
					while((delivery = consumer.nextDelivery())!=null){
						String msg = new String(delivery.getBody());
						if(msg.equals(removeMsg)){
							channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		},"removeQueueMsg_" + queueName + "_" + removeMsg).start();
	}
}
