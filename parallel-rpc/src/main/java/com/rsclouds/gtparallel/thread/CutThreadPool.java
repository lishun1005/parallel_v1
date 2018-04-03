package com.rsclouds.gtparallel.thread;

import com.rscloud.ipc.rpc.api.dto.CutJobDto;
import com.rscloud.ipc.rpc.api.dto.MapManageDto;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CutThreadPool {
	private static BlockingQueue<Runnable>  priorityQueue= new PriorityBlockingQueue<Runnable>();
	
	private static ThreadPoolExecutor poolExecutor =
			new ThreadPoolExecutor(3, 3, 60, TimeUnit.SECONDS, priorityQueue){
				@Override
				protected void beforeExecute(Thread t, Runnable r) {
					super.beforeExecute(t, r);
					CutThread cutThread = (CutThread)r;
					t.setName("CutThreadPool_" + cutThread.getJobid());//rename threadName
				}
			};
	
	public static boolean remove(String id){
		CutJobDto cutJobDto = new CutJobDto();
		cutJobDto.setId(id);
		CutThread oldCt = new CutThread(cutJobDto, null);
		return priorityQueue.remove(oldCt);
	}
	public static void execute(CutJobDto c, MapManageDto m){
		CutThread ct = new CutThread(c, m);
		poolExecutor.execute(ct);
	}
}
