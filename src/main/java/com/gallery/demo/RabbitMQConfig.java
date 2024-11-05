package com.gallery.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.core.Queue;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.thumbnailRequestQueue}")
    private String requestQueueName;

    @Value("${app.rabbitmq.thumbnailResponseQueue}")
    private String responseQueueName;

    @Bean
    public Queue requestQueue() {
        return new Queue(requestQueueName, true);
    }

    @Bean
    public Queue responseQueue() {
        return new Queue(responseQueueName, true);
    }
}
