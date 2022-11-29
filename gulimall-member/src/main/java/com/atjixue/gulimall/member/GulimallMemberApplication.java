package com.atjixue.gulimall.member;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 想要远程调用别的服务
 * 1.引入openfeign
 * 2、编写一个接口，告诉springcloud这个接口需要调用远程服务
 *   1声明接口的每一个方法都是调用哪个远程服务的那个请求
 * 3、开启远程调用功能
 * */
@SpringBootApplication
@MapperScan("com.atjixue.gulimall.member.dao")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.atjixue.gulimall.member.feign")
@EnableRedisHttpSession
public class GulimallMemberApplication {
	public static void main(String[] args) {
		SpringApplication.run(GulimallMemberApplication.class, args);
	}

}
