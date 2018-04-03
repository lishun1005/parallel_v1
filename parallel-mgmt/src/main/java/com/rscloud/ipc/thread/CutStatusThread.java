package com.rscloud.ipc.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CutStatusThread implements Runnable{
	private final static Logger logger = LoggerFactory.getLogger(CutStatusThread.class);
	private String queueName;
	public CutStatusThread(String queueName) {
		this.queueName = queueName;
	}	
	@Override
	public void run() {
		/*CutService cutService =(CutService)SpringApplication.getSpringBean("cutService");
		Channel channel = RabbitmqClient.getChannel();
		QueueingConsumer consumer = new QueueingConsumer(channel);
		try {
			channel.basicConsume(this.queueName,false, consumer);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Delivery delivery=null;
		String firstJobid = "";
		String jobid = "";
		boolean isRemove = false;
		String geoRange = "";
		try {
			while((delivery = consumer.nextDelivery())!=null){
				jobid = new String(delivery.getBody());
				if(!isRemove && "".equals(firstJobid)){
					firstJobid = jobid;
				}else{
					isRemove = false;
					if(firstJobid.equals(jobid)){//第一条睡眠30s
						thread.sleep(30000);
					}
				}
				CutJobDto job=new CutJobDto();
				Map<String,Object> result=cutService.progress(jobid);//查询job状态
				if("2001".equals(result.get("code"))){
					job.setJobid(jobid);
					if("ACCEPTED".equals(result.get("state")) || "RUNNING".equals(result.get("state"))){//接收和运行状态需要重新加入队列
						RabbitmqClient.getChannel().basicPublish("", this.queueName, 
								MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
					}else if("FAILED".equals(result.get("state"))){//成功或失败不重新加入队列，不做处理
						if(result.get("log")!=null){
							job.setLog(result.get("log").toString());
						}else{
							job.setLog(result.toString());
						}
						if(jobid.equals(firstJobid)){
							firstJobid="";
							isRemove = true;
						}
					}else if("SUCCEEDED".equals(result.get("state"))){
						
						job.setGeowebcacheUrl(result.get("url").toString());
						job.setLog("切片成功");
						if(jobid.equals(firstJobid)){
							firstJobid="";
							isRemove = true;
						}
					}else{
						job.setStatus("FAILED");
						if(result.get("errorMessage")!=null){
							job.setLog(result.get("errorMessage").toString());
						}else{
							job.setLog(result.toString());
						}
						if(result.get("errorCode") != null && "2004".equals(result.get("errorCode").toString())){
							logger.info("jobid={} not exit",jobid);//jobid not exit 不重新加入队列
							if(jobid.equals(firstJobid)){
								firstJobid="";
								isRemove = true;
							}
						}else{
							RabbitmqClient.getChannel().basicPublish("", this.queueName, 
									MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
						}
					}
					if(result.get("start_time") != null){
						job.setStartTime(new Date(Long.parseLong((String)result.get("start_time"))));
					}
					if(result.get("end_time") != null){
						job.setEndTime(new Date(Long.parseLong((String)result.get("end_time"))));
					}
					if(result.get("state") != null){
						job.setStatus(result.get("state").toString());
					}
					if(result.get("progress") != null){
						job.setProgress(Integer.valueOf(result.get("progress").toString()));
					}
					if(result.get("geo_range") != null){
						geoRange = result.get("geo_range").toString();
					}
					cutService.updateByJobid(job,geoRange);
				}else{
					RabbitmqClient.getChannel().basicPublish("", this.queueName, 
							MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
					logger.info(result.get("errorMessage").toString());
				}
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);  //处理完成才移除队列
				
			}
		} catch (Exception e) {
			try {
				RabbitmqClient.getChannel().basicPublish("", this.queueName, 
						MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); //处理完成才移除队列
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}*/
	}

	
}
