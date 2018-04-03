package com.rsclouds.gtparallel.core.gtdata.cutting;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeoutException;

import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.conf.Configuration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rsclouds.gtparallel.core.common.CoreConfig;


public class AddImageRabbitmq {
	private static long parserLongTime(String path) {
		long time = 0;
		String strTime = ParserTime.parserTimeMilliSeconds(path);
		if (strTime != null) {
			time = Long.parseLong(strTime);
		}
		return time;
	}
	
	private static void usage() {
		System.out.println("usage: <meta table> <path dir> <date yyyyMMdd> <test true/false> <dateflag true/false>");
	}
	
	public static void main(String[] args) {
		if (args.length < 5) {
			usage();
			System.exit(0);
		}
		Configuration conf = HBaseConfiguration.create();
		try {
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(CoreConfig.RABBITMQ_HOST);
			factory.setPort(CoreConfig.RABBITMQ_PORT);
			factory.setUsername(CoreConfig.RABBITMQ_USER);
			factory.setPassword("rsclouds@012");
			factory.setVirtualHost(CoreConfig.RABBITMQ_VIRTUAL_HOST);
			Connection connection = factory.newConnection();
			Channel channel = connection.createChannel();
			channel.queueDeclare(CoreConfig.RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME, true, false, false, null);
			System.out.println("============rabbitmq config============");
			System.out.println("host: " + CoreConfig.RABBITMQ_HOST);
			System.out.println("port: " + CoreConfig.RABBITMQ_PORT);
			System.out.println("user: " + CoreConfig.RABBITMQ_USER);
			System.out.println("vortul_host: " + CoreConfig.RABBITMQ_VIRTUAL_HOST);
			System.out.println("queue: " + CoreConfig.RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME);
			System.out.println("==========rabbitmq config end===========");
			
			HTable metaHTable = new HTable(conf, args[0]);
			String prefix = args[1];
			while (prefix.endsWith("/")) {
				prefix = prefix.substring(0, prefix.length() -1);
			}
			boolean test = Boolean.parseBoolean(args[3]);
			boolean dateBoolean = Boolean.parseBoolean(args[4]);
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			Date date = format.parse(args[2]);
			long longTime = date.getTime();
			Scan scan = new Scan();
			scan.setStartRow(Bytes.toBytes(prefix + "//"));
			scan.setStopRow(Bytes.toBytes(prefix + "/{"));
			ResultScanner scanner = metaHTable.getScanner(scan);
			for (Result res : scanner) {
				String strRowKey = new String(res.getRow());
				strRowKey = strRowKey.replaceAll("//", "/");
				if (dateBoolean) {
					if (longTime == parserLongTime(strRowKey) && strRowKey.contains("GF1_PMS")) {
						if (!test) {
							channel.basicPublish("", CoreConfig.RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME, 
									MessageProperties.PERSISTENT_TEXT_PLAIN, strRowKey.getBytes());
						}
						System.out.println(strRowKey);
					}else if (test) {
						System.out.println(strRowKey + " isn't need to input to rabbitmq");
					}
				}else if (strRowKey.contains("GF1_PMS")) {
					if (!test) {
						channel.basicPublish("", CoreConfig.RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME, 
								MessageProperties.PERSISTENT_TEXT_PLAIN, strRowKey.getBytes());
					}
					System.out.println(strRowKey);
				}else if (test) {
					System.out.println(strRowKey + " isn't need to input to rabbitmq");
				}
			}
			metaHTable.close();
			channel.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			e.printStackTrace();
		}
		
		System.exit(0);
	}
}
