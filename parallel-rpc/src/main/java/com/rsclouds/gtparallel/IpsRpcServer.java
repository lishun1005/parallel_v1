package com.rsclouds.gtparallel;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.web.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

import java.io.IOException;

/**
 * 服务启动
 * @author KingYiu
 *
 */
@SpringBootApplication
@ImportResource(value={ "classpath:app.xml" })
public class IpsRpcServer extends SpringBootServletInitializer{
	private final static Logger logger = LoggerFactory.getLogger(IpsRpcServer.class);
	/*	@Override
    protected SpringApplicationBuilder configure(
            SpringApplicationBuilder application) {
        return application.sources(IpsRpcServer.class);
    }
	@Override
	protected WebApplicationContext run(SpringApplication application) {
		return (WebApplicationContext) application.run();
	};*/
	public static void main(String[] args) {
		SpringApplication.run(IpsRpcServer.class, args);
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
