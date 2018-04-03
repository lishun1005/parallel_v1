package com.rsclouds.gtparallel.core.gtdata.common;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;

import redis.clients.jedis.Jedis;

import com.rsclouds.gtparallel.gtdata.utills.GtDataConfig;
import com.rsclouds.gtparallel.gtdata.utills.GtDataUtils;


public class RedisUtils {
	
	public static void redisDirCheck(Jedis jedis, String gtPath) throws UnsupportedEncodingException{
		gtPath=gtPath.substring(0, gtPath.lastIndexOf("//"));
		gtPath=GtDataUtils.replaceLast(gtPath,"/","//");
		redisFileCheck(jedis, gtPath);
	}
	
	public static void redisDirCheck(String gtPath,String redisHost) throws UnsupportedEncodingException{
		gtPath=gtPath.substring(0, gtPath.lastIndexOf("//"));
		gtPath=GtDataUtils.replaceLast(gtPath,"/","//");
		redisFileCheck(gtPath, redisHost);
	}
	
	public static void redisFileCheck(String gtPath,String redisHost){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT);
		if(jedis.exists(gtPath)){
			String value = jedis.get(gtPath);
			if(value.endsWith("1,")){
				value = GtDataUtils.replaceLast(value,"1,","0,");
				jedis.set(gtPath, value);
			}
		}
		jedis.close();
	}
	
	public static void redisFileCheck(Jedis jedis, String gtPath){
		if(!jedis.isConnected()) {
			jedis.connect();
			System.err.println(new Date(System.currentTimeMillis()) + " redis reconnetc.");
		}
		if(jedis.exists(gtPath)){
			String value = jedis.get(gtPath);
			if(value.endsWith("1,")){
				value = GtDataUtils.replaceLast(value,"1,","0,");
				jedis.set(gtPath, value);
			}
		}
	}
	
	public static Long redisDel(String redisHost,String outputPath){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT);
		try{		
			return jedis.del(outputPath);
		}finally{
			if(jedis!= null){
				jedis.close();
			}
		}		
	}
	
	public static Long redisDel(String redisHost,String... outputPath){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT);
		try{		
			for(String str : outputPath){
				jedis.del(str);
			}
		}finally{
			if(jedis!= null){
				jedis.close();
			}
		}	
		return 0L;
	}
	
	public static Long redisDel(String redisHost,List<byte[]> outputPath){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT);
		try{		
			for(byte[] str : outputPath){
				jedis.del(str);
			}
		}finally{
			if(jedis!= null){
				jedis.close();
			}
		}	
		return 0L;
	}

	/**
	 * @param args
	 * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args) throws UnsupportedEncodingException {
		// TODO Auto-generated method stub
		redisDirCheck("/map/ssh/data/map/theme//京津冀_PM2_5_20140513","10.0.78.6");
//		Jedis jedis = new Jedis("10.0.78.6",6379);
//		Set keys = jedis.keys("*");//列出所有的key，查找特定的key如：redis.keys("foo") 
//        Iterator t1=keys.iterator() ; 
//        while(t1.hasNext()){ 
//            Object obj1=t1.next(); 
////            System.out.println(obj1); 
////            System.out.println(jedis.get((String) obj1)); 
////            System.out.println();
//            if(jedis.get((String) obj1).endsWith("1,")){
//            	System.out.println(obj1);
//            	System.out.println(jedis.get((String) obj1)); 
//            }           	
//        } 
	}

}
