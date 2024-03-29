package com.atjixue.gulimall.ware.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author LiuJixue
 * @Date 2022/9/17 21:39
 * @PackageName:com.atjixue.gulimall.ware.config
 * @ClassName: MyRabbitConfig
 * @Description: TODO
 * @Version 1.0
 */
@Configuration
public class MyRabbitConfig {
    // JSON 序列化机制
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }
//    @RabbitListener(queues = "stock.release.stock.queue")
//    public void handle(Message message){
//
//    }

    @Bean
    public Exchange stockEventExchange() {
        //String name, boolean durable, boolean autoDelete, Map<String, Object> arguments
        return new TopicExchange("stock-event-exchange", true, false);
    }
    @Bean
    public Queue stockReleaseStockQueue(){
        //String name, boolean durable, boolean exclusive, boolean autoDelete, Map<String, Object> arguments
        return new Queue("stock.release.stock.queue",true,false,false);
    }

    @Bean
    public Queue stockDelayQueue(){
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","stock-event-exchange");
        arguments.put("x-dead-letter-routing-key","stock.release");
        arguments.put("x-message-ttl",120000);
        return new Queue("stock.delay.queue",true,false,false,arguments);
    }
    @Bean
    public Binding stockReleaseBinding(){
        //String destination, DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments
        return new Binding("stock.release.stock.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.release.#",
                null);
    }

    @Bean
    public Binding stockLockedBinding(){
        //String destination, DestinationType destinationType, String exchange, String routingKey, Map<String, Object> arguments
        return new Binding("stock.delay.queue",
                Binding.DestinationType.QUEUE,
                "stock-event-exchange",
                "stock.locked",
                null);
    }

}
