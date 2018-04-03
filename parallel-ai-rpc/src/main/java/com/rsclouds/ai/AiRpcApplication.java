package com.rsclouds.ai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@MapperScan("com.rsclouds.ai.mapper")
@ImportResource(value = {"classpath:app-dubbo.xml"})
public class AiRpcApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiRpcApplication.class, args);
	}
}
