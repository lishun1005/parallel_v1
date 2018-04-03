package com.rscloud.ipc.shiro;


import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.ExcessiveAttemptsException;
import org.apache.shiro.authc.credential.SimpleCredentialsMatcher;
import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * User: xiangsx
 * <p>
 * Date: 2015-03-17
 * <p>
 * Version: 1.0
 */
public class RetryLimitMatcher extends
	SimpleCredentialsMatcher {

	private Cache<String, AtomicInteger> passwordRetryCache;
	protected final Logger logger = LoggerFactory.getLogger(RetryLimitMatcher.class);
	public RetryLimitMatcher(CacheManager cacheManager) {
		passwordRetryCache = cacheManager.getCache("passwordRetryCache");
	}

	@Override
	public boolean doCredentialsMatch(AuthenticationToken token,
			AuthenticationInfo info) {
		String username = (String) token.getPrincipal();
		AtomicInteger retryCount = passwordRetryCache.get(username);
		if (retryCount == null) {
			retryCount = new AtomicInteger(0);
			passwordRetryCache.put(username, retryCount);
		}else{
			retryCount.incrementAndGet();
			int count = retryCount.get();
			passwordRetryCache.put(username, retryCount);
			if (count > 10) {
				throw new ExcessiveAttemptsException();
			}
		}
		boolean matches = super.doCredentialsMatch(token, info);
		if (matches) {
			passwordRetryCache.remove(username);// clear retry count
		}
		return matches;
	}
}
