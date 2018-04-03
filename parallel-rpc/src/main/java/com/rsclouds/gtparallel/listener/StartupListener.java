package com.rsclouds.gtparallel.listener;


import com.rscloud.ipc.rpc.api.service.CutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;

/**
 * 启动监听器
 * @author
 */
@Service
public class StartupListener implements ApplicationListener<ContextRefreshedEvent> {
	public static Logger logger = LoggerFactory.getLogger(StartupListener.class);
	@Autowired
	@Lazy
	private CutService cutService;
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() == null) {
			cutService.initUndoneCutjob();
		}
	}
}
