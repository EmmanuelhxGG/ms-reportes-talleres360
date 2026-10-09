package com.talleres360.report.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talleres360.report.service.ReportService;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true")
public class ConsumidorKafka {
  private final ReportService service;
  private final ObjectMapper json;
  private final Validator validator;

  @KafkaListener(
      topics = "${app.kafka.topic:orders.events}",
      groupId = "${spring.kafka.consumer.group-id}")
  public void consumir(String mensaje) throws java.io.IOException {
    var evento = json.readValue(mensaje, ReportService.EventInput.class);
    if (!validator.validate(evento).isEmpty())
      throw new IllegalArgumentException("Evento inválido");
    service.ingest(evento);
  }
}
