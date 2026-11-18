package com.talleres360.report;

import com.talleres360.report.service.ConsumidorEventosKafka;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "KAFKA_ENABLED=true",
        "spring.kafka.listener.auto-startup=false",
        "app.internal-key=test-internal"
})
@AutoConfigureMockMvc
class KpisKafkaTest {
    @Autowired ConsumidorEventosKafka consumidor;
    @Autowired MockMvc http;

    @Test
    void consolidaEntregasDesdeKafkaYConservaEventosAnteriores() throws Exception {
        consumir("creacion-nueva", 1, "CREADA", "2026-10-31T22:00:00Z", "");
        consumir("entrega-nueva", 1, "ENTREGADA", "2026-11-02T02:00:00Z", """
                , "productosVenta": [{"productoId":7,"nombre":"Filtro","cantidad":3}]
                """);
        consumir("entrega-nueva", 1, "ENTREGADA", "2026-11-02T02:00:00Z", """
                , "productosVenta": [{"productoId":7,"nombre":"Filtro","cantidad":3}]
                """);
        consumir("entrega-anterior", 2, "ENTREGADA", "2026-11-02T02:30:00Z", "");
        consumir("cancelacion", 3, "CANCELADA", "2026-11-02T02:45:00Z", "");
        consumir("entrega-limite", 4, "ENTREGADA", "2026-11-02T03:00:00Z", "");

        var consulta = get("/api/reports/kpis")
                .param("from", "2026-11-01T03:00:00Z")
                .param("to", "2026-11-02T03:00:00Z")
                .param("zone", "America/Santiago");
        http.perform(consulta).andExpect(status().isUnauthorized());
        http.perform(consulta.header("X-Internal-Key", "test-internal"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen.deliveredOrders").value(2))
                .andExpect(jsonPath("$.resumen.revenue").value(2000))
                .andExpect(jsonPath("$.ventasPorDia[0].fecha").value("2026-11-01"))
                .andExpect(jsonPath("$.tiemposPorDia[0].promedioMinutos").value(1680))
                .andExpect(jsonPath("$.entregasMedidas").value(1))
                .andExpect(jsonPath("$.pedidosConDetalle").value(1))
                .andExpect(jsonPath("$.productos[0].productoId").value(7))
                .andExpect(jsonPath("$.productos[0].unidades").value(3));
    }

    private void consumir(String id, long pedido, String tipo, String fecha, String detalle) throws Exception {
        consumidor.consumir("""
                {"evento":{"eventId":"%s","orderId":%d,"type":"%s","actor":"test",
                  "occurredAt":"%s","status":"%s","total":1000},
                 "stockConfirmado":true%s}
                """.formatted(id, pedido, tipo, fecha, tipo, detalle));
    }
}
