package com.talleres360.report.service;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "KAFKA_ENABLED", havingValue = "true")
class ConfiguracionErroresKafka {
  @Bean
  DefaultErrorHandler erroresKafka(KafkaTemplate<String, String> kafka) {
    var recuperador = new DeadLetterPublishingRecoverer(
        kafka, (registro, error) -> new TopicPartition("orders.events.reporteria.DLT", registro.partition()));
    recuperador.setFailIfSendResultIsError(true);
    return new DefaultErrorHandler(recuperador, new FixedBackOff(1000L, 2L));
  }
}
