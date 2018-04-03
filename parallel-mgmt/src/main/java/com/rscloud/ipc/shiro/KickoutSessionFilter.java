package com.rscloud.ipc.shiro;


import java.io.Serializable;
import java.util.Deque;
import java.util.LinkedList;

import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.DefaultSessionKey;
import org.apache.shiro.session.mgt.SessionManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.web.filter.AccessControlFilter;
import org.apache.shiro.web.util.WebUtils;

import com.rscloud.ipc.dto.SysUserShiroDto;

/**
 * <p>User: Zhang Kaitao
 * <p>Date: 14-2-18
 * <p>Version: 1.0
 */
public class KickoutSessionFilter extends AccessControlFilter {

    private String kickoutUrl; //踢出后到的地址
    //private boolean kickoutAfter = false; //踢出之前登录的/之后登录的用户 默认踢出之前登录的用户，-----无法做到踢出之后登陆用户（无法获取用户何时推出浏览器）
    private int maxSession = 1; //同一个帐号最大会话数 默认1

    private SessionManager sessionManager;
    private Cache<String, Deque<Serializable>> cache;

   public void setKickoutUrl(String kickoutUrl) {
        this.kickoutUrl = kickoutUrl;
    }

   /*  public void setKickoutAfter(boolean kickoutAfter) {
        this.kickoutAfter = kickoutAfter;
    }*/

    public void setMaxSession(int maxSession) {
        this.maxSession = maxSession;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public void setCacheManager(CacheManager cacheManager) {
        this.cache = cacheManager.getCache("shiro-kickout-session");//该缓存失效时间 >= session失效时间
    }

    @Override
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) throws Exception {
        return false;
    }

    @Override
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
        Subject subject = getSubject(request, response);
        if(!subject.isAuthenticated() && !subject.isRemembered()) {
            return true;//如果没有登录，直接进行之后的流程
        }
        Session session = subject.getSession();
        SysUserShiroDto user = (SysUserShiroDto) subject.getPrincipal();
        
        String username = user.getUsername();
        Serializable sessionId = session.getId();
        Deque<Serializable> deque = cache.get(username + "-" + user.getUserType());//TODO 同步控制
        if(deque == null) {
            deque = new LinkedList<Serializable>();
            //cache.put(username, deque);
        }
        //如果队列里没有此sessionId，且用户没有被踢出；放入队列
        if(!deque.contains(sessionId) && session.getAttribute("kickout") == null) {
            deque.push(sessionId);
            cache.put(username + "-" + user.getUserType(), deque);
        }
        while(deque.size() > maxSession) {//如果队列里的sessionId数超出最大会话数，开始踢人
            Serializable kickoutSessionId = deque.removeLast();
            try {
                Session kickoutSession = sessionManager.getSession(new DefaultSessionKey(kickoutSessionId));
                if(kickoutSession != null) {
                    kickoutSession.setAttribute("kickout", true);//设置会话的kickout属性表示踢出了
                }
            } catch (Exception e) {//ignore exception 
            	//e.printStackTrace();
            }
        }
        if (session.getAttribute("kickout") != null) {//如果被踢出了，直接退出，重定向到踢出后的地址
            try {
                subject.logout(); //会话被踢出了
            } catch (Exception e) { //ignore
            	e.printStackTrace();
            }
            saveRequest(request);
            WebUtils.issueRedirect(request, response, kickoutUrl);
            return false;
        }

        return true;
    }
}
