package com.ile.alert.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic weatherUpdatesTopic() {
        return TopicBuilder
                .name("weather-updates")
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic alertNotificationsTopic() {
        return TopicBuilder
                .name("alert-notifications")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
