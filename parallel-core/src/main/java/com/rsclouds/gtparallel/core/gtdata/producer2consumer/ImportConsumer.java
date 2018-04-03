package com.rsclouds.gtparallel.core.gtdata.producer2consumer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeoutException;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;

import com.rsclouds.gtparallel.core.common.CoreConfig;
import com.rsclouds.gtparallel.core.gtdata.operation.Import;
import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;

public class ImportConsumer implements Runnable{
	private String outputRoot = null;
	private BlockingQueue<String> storage = null;
	private HTable metaTable = null;
	private HTable resTable = null;
	private Import importFile = null;

	private String fileFinishFlagStr = null;
	private String backDir = "/data/sdm/ftp/rsmartftp_succed";
	private String errorDir = null;
	private final static String QUEUE_NAME = CoreConfig.RABBITMQ_QUEUE_NAME;
	private final static String REALTIMECHINA_PRODUCE_QUEUE_NAME = CoreConfig.RABBITMQ_REALTIMECHINA_PRODUCE_QUEUE_NAME;
	private static Channel channel;
	private short defaultReplacation;
	
	private int timeout;
	
	public ImportConsumer(String backDir, String outputDir, BlockingQueue<String> storage, String fileFinishFlagStr, int timeout) {
		this.outputRoot = outputDir;
		while(this.outputRoot.endsWith("/")) {
			this.outputRoot = this.outputRoot.substring(0, this.outputRoot.length()-1);
		}
		this.storage = storage;
		this.fileFinishFlagStr = fileFinishFlagStr;
		this.backDir = backDir;
		errorDir = CoreConfig.ERROR_DIR_PATH;
		while(errorDir.endsWith("/")) {
			errorDir = errorDir.substring(0, errorDir.length()-1);
		}
		File file = new File(errorDir);
		if(!file.exists()) {
			file.mkdirs();
		}
		this.timeout = timeout;
		defaultReplacation = 3;
		
	}
	
	public ImportConsumer(String backDir, String outputDir, BlockingQueue<String> storage,
			String fileFinishFlagStr, int timeout, short defaultReplacation) {
		this.outputRoot = outputDir;
		while(this.outputRoot.endsWith("/")) {
			this.outputRoot = this.outputRoot.substring(0, this.outputRoot.length()-1);
		}
		this.storage = storage;
		this.fileFinishFlagStr = fileFinishFlagStr;
		this.backDir = backDir;
		errorDir = CoreConfig.ERROR_DIR_PATH;
		while(errorDir.endsWith("/")) {
			errorDir = errorDir.substring(0, errorDir.length()-1);
		}
		File file = new File(errorDir);
		if(!file.exists()) {
			file.mkdirs();
		}
		this.timeout = timeout;
		this.defaultReplacation = defaultReplacation;
		
	}
	
	
	public boolean init(){
		Configuration conf = HBaseConfiguration.create();
		try {
			System.setErr(new PrintStream(new File(CoreConfig.ERROR_LOG_PATH)));
			System.setOut(new PrintStream(new File(CoreConfig.ACCESS_LOG_PATH)));
			System.out.println("init meta start");
			metaTable = new HTable(conf, GtDataConfig.TABLE_NAME.META_TABLE.getStrVal());
			System.out.println("init res start");
			resTable = new HTable(conf, GtDataConfig.TABLE_NAME.RES_TABLE.getStrVal());
			importFile = new Import(timeout);
			System.out.println("init rabbitmq start");
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(CoreConfig.RABBITMQ_HOST);
			factory.setPort(CoreConfig.RABBITMQ_PORT);
			factory.setUsername(CoreConfig.RABBITMQ_USER);
			factory.setPassword("rsclouds@012");
			factory.setVirtualHost(CoreConfig.RABBITMQ_VIRTUAL_HOST);
			Connection connection = factory.newConnection();
			channel = connection.createChannel();
			channel.queueDeclare(QUEUE_NAME, true, false, false, null);
			channel.queueDeclare(REALTIMECHINA_PRODUCE_QUEUE_NAME, true, false, false, null);
			System.out.println("init sucessed");
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println("init failed");
			return false;
		} catch (TimeoutException e) {
			e.printStackTrace();
			System.err.println("init failed");
			return false;
		}
	}
	
	
	
	@Override
	public void run() {
		boolean initFlag = false;
		for(int i = 0; i < 3; i ++) {
			if (init()) {
				initFlag = true;
				break;
			}
		}
		if(!initFlag) {
			return;
		}else {
			System.out.println("start import");
		}
		boolean fileCheck = false;
        while (true) {
        	fileCheck = false;
            String product = (String)storage.poll();
            
            if (product != null) {  
            	//System.out.println("[ImportConsumer::run]product= " + product);
            	File file = new File(product);
            	if(file.exists()) {
            		File dateParents = file.getParentFile();
            		File satelliteParents = dateParents.getParentFile();
            		String outputDir = outputRoot + "/" + satelliteParents.getName() + "/" + dateParents.getName();
            		InputStream in = null;
            		GZIPInputStream zip = null;
            		
					try {
						in = new FileInputStream(file);
						zip = new GZIPInputStream(in);
						TarArchiveInputStream tarInputStream = (TarArchiveInputStream) new ArchiveStreamFactory().createArchiveInputStream("tar", zip);
						tarInputStream.close();
						zip.close();
						zip = null;
						in.close();
						in = null;
						fileCheck = true;
					}catch (IOException e) {	
						try {
							if(zip != null) {
								zip.close();
								zip = null;
							}
							if(in != null) {
								in.close();
								in = null;
							}
						} catch (IOException e1) {}
					}catch (ArchiveException e) {
						try {
							if(zip != null) {
								zip.close();
								zip = null;
							}
							if(in != null) {
								in.close();
								in = null;
							}
						} catch (IOException e1) {
						}
					}
            		if(!fileCheck) {
            			System.err.println(new Date(System.currentTimeMillis()) + " file's data is error: " + product);
            			File fileFin = new File(file.getPath() + "." + fileFinishFlagStr);
            			File dstFileFin = new File(errorDir + "/" + file.getName() + "." + fileFinishFlagStr);
            			fileFin.renameTo(dstFileFin);
            			dstFileFin.setLastModified(System.currentTimeMillis());
            			
            			File dstFile = new File(errorDir + "/" + file.getName());
            			file.renameTo(dstFile);
            			dstFile.setLastModified(System.currentTimeMillis());
            			continue;
            		}
            		if (importFile.ImportFileToDir(metaTable, resTable, product, outputDir, defaultReplacation)) {
            			try {
            				String usersOutputDir = outputDir.substring(6) + "/" + file.getName();
							channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, usersOutputDir.getBytes());
							channel.basicPublish("", REALTIMECHINA_PRODUCE_QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, (outputDir + "/" + file.getName()).getBytes());
							File dateParentsBack = new File(backDir + "/" + satelliteParents.getName() + "/" + dateParents.getName());
                			if(!dateParentsBack.exists())
                				dateParentsBack.mkdirs();
                			
                			File fileFin = new File(file.getPath() + "." + fileFinishFlagStr);
                			File dstFileFin = new File(dateParentsBack.getPath() + "/" + file.getName() + "." + fileFinishFlagStr);
                			Files.move(Paths.get(fileFin.getPath()), Paths.get(dstFileFin.getPath()), StandardCopyOption.REPLACE_EXISTING);
                			dstFileFin.setLastModified(System.currentTimeMillis());
                			
                			File dstFile = new File(dateParentsBack.getPath() + "/" + file.getName());
                			Files.move(Paths.get(file.getPath()), Paths.get(dstFile.getPath()), StandardCopyOption.REPLACE_EXISTING);                					
                			dstFile.setLastModified(System.currentTimeMillis());
                			System.out.println(new Date(System.currentTimeMillis()) + " file input sucessed: " + usersOutputDir);                				                			
                			
                			
						} catch (IOException e) {
							e.printStackTrace();
							System.err.println(product + "input queue error" );
						}
            		}else {
            			System.err.println(product + "input gtdata error" );
            		}
            	}
            }                
        }
	}

}
