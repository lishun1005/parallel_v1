package com.rsclouds.gtparallel.core.gtdata.producer2consumer;

import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Storage<T> {
	private BlockingQueue<T> queue;
	
	public Storage(){
		queue = new LinkedBlockingQueue<T>(500);
	}
	
	public Storage(int capacity) {
		queue = new LinkedBlockingQueue<T>(capacity);
	}
	
	public synchronized boolean push(T e){
		try {
			queue.put(e);
			return true;
		} catch (InterruptedException e1) {
			return false;
		}
		
	}
	
	public synchronized T pop() {
		return queue.poll();
	}
	
	public static void main(String[] args) {
		System.out.println("/users/rscloudmart/data".substring(6));
		System.out.println(new Date(Long.parseLong("1440582350451")));
	}
}
