package com.rsclouds.gtparallel.gtdata.utills;

import java.io.UnsupportedEncodingException;
import java.util.List;

import redis.clients.jedis.Jedis;

/**
 * redis工具类，用于更新redis缓存
 * @author root
 *
 */
public class RedisUtils {
	
	public static void redisDirCheck(String redisHost,String gtPath) throws UnsupportedEncodingException{
		gtPath=gtPath.substring(0, gtPath.lastIndexOf("//"));
		gtPath=GtDataUtils.replaceLast(gtPath,"/","//");
		redisFileCheck(redisHost,gtPath);
	}
	
	public static void redisFileCheck(String redisHost,String gtPath){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT, 30000);
		if(jedis.exists(gtPath)){
			String value = jedis.get(gtPath);
			if(value.endsWith("1,")){
				value = GtDataUtils.replaceLast(value,"1,","0,");
				jedis.set(gtPath, value);
			}
		}
		jedis.close();
	}
	
	public static Long redisDel(String redisHost,String outputPath){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT, 30000);
		try{		
			return jedis.del(outputPath);
		}finally{
			if(jedis!= null){
				jedis.close();
			}
		}		
	}
	
	public static Long redisDel(String redisHost,String... outputPath){
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT, 30000);
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
		Jedis jedis = new Jedis(redisHost,GtDataConfig.REDIS_PORT, 30000);
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
		redisDirCheck("10.0.78.6","/map/ssh/data/map/theme//京津冀_PM2_5_20140513");
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
