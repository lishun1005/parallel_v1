package com.rscloud.ipc;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;

/**
 * 服务启动
 * @author KingYiu
 *
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class,HibernateJpaAutoConfiguration.class, RedisAutoConfiguration.class})
@ImportResource(value={ "classpath:app.xml" })
public class IpsWebServer extends SpringBootServletInitializer{
	private final static Logger logger = LoggerFactory.getLogger(IpsWebServer.class);
	public static void main(String[] args) {
		SpringApplication.run(IpsWebServer.class, args);
		while (true) {
			char c;
			try {
				c = (char) System.in.read();
				if (c == '\n') {
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);;
				System.exit(-1);
			}

		}
	}
}
