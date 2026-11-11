package com.talleres360.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "KAFKA_ENABLED", havingValue = "true")
public class ConsumidorEventosKafka {
  private final ObjectMapper json;
  private final Validator validator;
  private final ReportService reportes;

  @KafkaListener(topics = "orders.events", groupId = "talleres360-reporteria")
  public void consumir(String mensaje) throws java.io.IOException {
    var contenido = json.readTree(mensaje);
    if (contenido == null || !contenido.path("evento").isObject()
        || !contenido.path("stockConfirmado").isBoolean()) {
      throw new IllegalArgumentException("El mensaje no contiene un evento de orden válido");
    }
    var evento = json.treeToValue(contenido.get("evento"), ReportService.EventInput.class);
    if (!validator.validate(evento).isEmpty()) {
      throw new IllegalArgumentException("Datos inválidos en el evento de orden");
    }
    if (contenido.get("stockConfirmado").booleanValue()) reportes.ingest(evento);
  }
}
