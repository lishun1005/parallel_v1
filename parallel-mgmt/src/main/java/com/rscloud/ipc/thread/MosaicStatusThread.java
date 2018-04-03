package com.rscloud.ipc.thread;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.QueueingConsumer.Delivery;
import com.rscloud.ipc.rabbitmq.RabbitmqClient;
import com.rscloud.ipc.rpc.api.dto.MosaicJobDto;
import com.rscloud.ipc.rpc.api.result.ResultBean;
import com.rscloud.ipc.rpc.api.result.ResultCode;
import com.rscloud.ipc.rpc.api.service.MosaicService;
import com.rscloud.ipc.utils.SpringApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class MosaicStatusThread implements Runnable{
	private final static Logger logger = LoggerFactory.getLogger(MosaicStatusThread.class);
	private String queueName;
	public MosaicStatusThread(String queueName) {
		this.queueName = queueName;
	}	
	@Override
	public void run() {
		MosaicService mosaicService =(MosaicService) SpringApplication.getSpringBean("mosaicService");
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
		try {
			while((delivery = consumer.nextDelivery())!=null){
				jobid = new String(delivery.getBody());
				if(!isRemove && "".equals(firstJobid)){
					firstJobid = jobid;
				}else{
					isRemove = false;
					if(firstJobid.equals(jobid)){
						Thread.sleep(30000);
					}
				}
				MosaicJobDto job = new MosaicJobDto();
				ResultBean<Map<String, Object>> resultBean =  mosaicService.progressByHbase(jobid);
				if(Objects.equals(resultBean.getCode(), ResultCode.OK)){
					Map<String,Object> result =  resultBean.getResultData();
					if("ACCEPTED".equals(result.get("status")) || "RUNNING".equals(result.get("status")) ||
							"SUCCEEDED".equals(result.get("status"))){//接收和运行状态需要重新加入队列
						if(result.get("subStatus")!=null && result.get("subProgress")!=null){//判断子状态
							String subStatus=(String)result.get("subStatus");
							Integer subProgress=Integer.valueOf((String)result.get("subProgress"));
							List<Map<String,Object>> subList=mosaicService.getMosaicSub(jobid, subStatus);
							if(subList.size()==0){
								logger.error("jobid={},subStatus={},不存在记录",jobid,subStatus);
							}else if(subList.size()==1){
								Map<String,Object> subMosaic=subList.get(0);
								String subId=subMosaic.get("id").toString();
								Integer sortOrder=(Integer)subMosaic.get("sort_order");
								mosaicService.updateMosaicJobSub(subId, subProgress,sortOrder,jobid);
							}else{
								logger.error("jobid={},subStatus={},存在多条记录",jobid,subStatus);
							}
						}
						if("SUCCEEDED".equals(result.get("status"))){
							job.setLog("");
							if(jobid.equals(firstJobid)){
								firstJobid="";
								isRemove = true;
							}
						}else{
							RabbitmqClient.getChannel().basicPublish("", this.queueName,
									MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
						}
					}else if("FAILED".equals(result.get("status"))){//成功或失败不重新加入队列，不做处理
						if(result.get("log")!=null){
							job.setLog(result.get("log").toString());
						}else{
							job.setLog(result.toString());
						}
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
					job.setJobid(jobid);
					if(result.get("start_time")!=null){
						String startTime=(String)result.get("start_time");
						if(10 == startTime.length()){
							job.setStartTime(new Date(Long.parseLong(startTime + "000")));
						}else{
							job.setStartTime(new Date(Long.parseLong(startTime)));
						}
					}
					if(result.get("end_time")!=null){
						String endTime=(String)result.get("end_time");
						if(10 == endTime.length()){
							job.setEndTime(new Date(Long.parseLong(endTime + "000")));
						}else{
							job.setEndTime(new Date(Long.parseLong(endTime)));
						}
					}
					if(result.get("status")!=null){
						job.setStatus(result.get("status").toString());
					}
					mosaicService.updateByJobid(job);
				}else{
					RabbitmqClient.getChannel().basicPublish("", this.queueName,
							MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
					logger.info(resultBean.getMessage());
				}
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false); //处理完成才移除队列
			}
		} catch (Exception e) {
			try {
				RabbitmqClient.getChannel().basicPublish("", this.queueName, 
						MessageProperties.PERSISTENT_TEXT_PLAIN, jobid.getBytes());
				channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);  //处理完成才移除队列
			} catch (IOException e1) {
				e1.printStackTrace();
				logger.error(e1.getMessage());
			}
			e.printStackTrace();
			logger.error(e.getMessage());
		}
		
	
	}

	
}
