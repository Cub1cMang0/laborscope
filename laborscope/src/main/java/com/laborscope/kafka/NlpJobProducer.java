package com.laborscope.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class NlpJobProducer {
    // Initialize variables required for creating a kafka producer
    private final String nlpTopic;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public record NlpJob(Long pageId, String url) {}

    public NlpJobProducer(
            @Value("${crawler.kafka.topic-nlp-jobs}") String nlpTopic,
            KafkaTemplate<String, Object> kafkaTemplate) {
        this.nlpTopic = nlpTopic;
        this.kafkaTemplate = kafkaTemplate;
    }
    // Topic nlp job url and id for nlp-service processing
    public void publish(Long pageId, String url)
    {
        // Send the topic and NlpJob
        kafkaTemplate.send(nlpTopic, new NlpJob(pageId, url));
    }
}
