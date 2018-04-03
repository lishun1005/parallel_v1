package com.rscloud.ipc.utils;


import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

public class CookieUtiles {
	public static Logger logger = LoggerFactory.getLogger(CookieUtiles.class);

	public static void addCookie(String name, String value,Integer maxAge,
			HttpServletResponse response) {
		addCookie(name, value, maxAge, null, null, response);
	}

	public static void addCookie(String name, String value, Integer maxAge,
			String domain, String path, HttpServletResponse response) {
		Assert.hasText(name, "cookieName must not be empty");
		Cookie cookie = new Cookie(name, value);
		if (StringUtils.isNoneBlank(domain)) {
			cookie.setDomain(domain);
		}
		if (StringUtils.isNoneBlank(path)) {
			cookie.setPath(path);
		}
		if (maxAge > -1) {
			cookie.setMaxAge(maxAge);
		}
		response.addCookie(cookie);
	}

	public static void removeCookie(HttpServletResponse response, String name) {
		addCookie(name, "", 0, null, null, response);
		if (logger.isDebugEnabled()) {
			logger.debug("Removed cookie with name [" + name + "]");
		}
	}

	public static String retrieveCookieValue(final HttpServletRequest request,
			String cookieName) {
		final Cookie cookie = org.springframework.web.util.WebUtils.getCookie(
				request, cookieName);
		return cookie == null ? null : cookie.getValue();
	}
}
