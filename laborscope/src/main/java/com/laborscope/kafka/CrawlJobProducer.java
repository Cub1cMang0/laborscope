package com.laborscope.kafka;

import org.springframework.stereotype.Service;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Service
public class CrawlJobProducer {
    // Extract topic from application.yml
    @Value ("${spring.kafka.topic-crawl-jobs")
    private String topic;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // Kafka Template wrapper
    public void KakfaProducerService(KafkaTemplate<String, String> kafkaTemplate)
    {
        this.kafkaTemplate = kafkaTemplate;   
    }
    // Topic seed url publisher for the kafka consumer
    private void publish(String url)
    {
        kafkaTemplate.send(topic, url);
    }
}
