package com.rscloud.ipc.conf;

import httl.web.springmvc.HttlViewResolver;
import org.springframework.boot.context.embedded.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.DelegatingFilterProxy;

/**
 * @author ceshi
 * @Title: ${file_name}
 * @Package ${package_name}
 * @Description: ${todo}
 * @date 2018/4/210:08
 */
@Configuration
public class WebConfig {
    @Bean
    public HttlViewResolver httlViewResolver()
    {
        HttlViewResolver resolver = new HttlViewResolver();
        resolver.setCache(false);
        resolver.setPrefix("");
        resolver.setSuffix(".httl");
        resolver.setContentType("text/html; charset=UTF-8");
        resolver.setExposeSessionAttributes(true);
        return resolver;
    }

    @Bean
    public FilterRegistrationBean shiroFilterRegistration() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new ShiroFilter());
        registration.addUrlPatterns("/*");
        registration.setName("shiroFilter");
        registration.setOrder(1);
        return registration;
    }
    class ShiroFilter extends DelegatingFilterProxy {

    }
}
