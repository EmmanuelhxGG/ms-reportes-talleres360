package com.talleres360.report.config;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.*;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class KafkaConfig {
  @Bean
  CommonErrorHandler erroresKafka(KafkaTemplate<String, String> template) {
    var recuperador =
        new DeadLetterPublishingRecoverer(
            template,
            (registro, error) ->
                new TopicPartition(registro.topic() + ".report.DLT", registro.partition()));
    recuperador.setFailIfSendResultIsError(true);
    return new DefaultErrorHandler(recuperador, new FixedBackOff(1000L, 2L));
  }
}
