package com.talleres360.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validator;
import java.util.List;
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
    if (!contenido.get("stockConfirmado").booleanValue()) return;

    var detalle = contenido.path("productosVenta");
    List<ReportService.ProductoVendido> productos = null;
    if (!detalle.isMissingNode() && !detalle.isNull()) {
      if (!detalle.isArray()) throw new IllegalArgumentException("El detalle de productos debe ser una lista");
      productos = json.convertValue(detalle, new TypeReference<>() {});
      if (productos.stream().anyMatch(producto -> producto == null || !validator.validate(producto).isEmpty())) {
        throw new IllegalArgumentException("Datos inválidos en los productos de la entrega");
      }
    }
    reportes.ingest(evento, productos);
  }
}
