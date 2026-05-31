package com.example.applicationservice.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@Slf4j
public class KafkaErrorHandlerConfig {
    private static final long RETRY_INTERVALS_MS = 1000;

    private static final int MAX_RETRIES = 3;

    @Bean
    public DefaultErrorHandler defaultErrorHandler(
            KafkaTemplate<Object, Object> dlqKafkaTemplate
    ){
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                dlqKafkaTemplate,
                (record, exception) -> {
                    String dlqTopic = record.topic() + ".dlq";

                    log.error(
                            "Sending message to DLQ. originalTopic={}, dlqTopic={}, partition={}, offset={}, key={}",
                            record.topic(),
                            dlqTopic,
                            record.partition(),
                            record.offset(),
                            record.key(),
                            exception
                    );
                    return new TopicPartition(dlqTopic, record.partition());
                }
        );

        FixedBackOff fixedBackOff = new FixedBackOff(
                RETRY_INTERVALS_MS,
                MAX_RETRIES
        );

        return new DefaultErrorHandler(recoverer, fixedBackOff);
    }
}
