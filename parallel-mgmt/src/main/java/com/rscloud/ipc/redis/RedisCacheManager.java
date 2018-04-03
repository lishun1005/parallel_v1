package com.rscloud.ipc.redis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheException;
import org.apache.shiro.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
* @ClassName: redis 缓存管理器   参照 shiro-redis实现，修改设置缓存失效时间
* @Description: TODO
* @author lishun 
* @date 2017年7月12日 下午5:03:33  
*
 */
public class RedisCacheManager implements CacheManager {
	private static final Logger logger = LoggerFactory
			.getLogger(RedisCacheManager.class);
	private final ConcurrentMap<String, Cache> caches = new ConcurrentHashMap<String, Cache>();//存储不同类型的权限，以'name-key'存储
	private RedisClientTemplate redisManager;
	
	private Integer expireSec;
	public Integer getExpireSec() {
		return expireSec;
	}
	public void setExpireSec(Integer expireSec) {
		this.expireSec = expireSec;
	}
	@Override
	public <K, V> Cache<K, V> getCache(String name) throws CacheException {
		Cache c = caches.get(name);
		if (c == null) {
			logger.info("Cache with name '{}' does not yet exist.  Creating now.", name);
			c = new RedisCache<K, V>(redisManager, name, expireSec);
			caches.put(name, c);
		}else{
			if (logger.isDebugEnabled()) {
				logger.debug("Using existing Redis_Cache named [" + name + "]");
			}
		}
		return c;
	}

	public RedisClientTemplate getRedisManager() {
		return redisManager;
	}

	public void setRedisManager(RedisClientTemplate redisManager) {
		this.redisManager = redisManager;
	}

}
