package com.talleres360.report;

import com.talleres360.report.service.ReportService;
import com.talleres360.report.service.ReportService.EventInput;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import java.math.BigDecimal;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = "INTERNAL_API_KEY=test-key")
@AutoConfigureMockMvc
class ReportServiceTest {
    @Autowired ReportService service;
    @Autowired MockMvc mvc;

    @Test
    void soloLasEntregasCuentanComoVentasYElEventoEsIdempotente() {
        Instant now = Instant.now();
        service.ingest(new EventInput("evt-1", 3L, "CREADA", "cliente@ejemplo.cl", null, now, "RECIBIDA", BigDecimal.ZERO));
        EventInput delivered = new EventInput("evt-2", 3L, "ENTREGADA", "operador@ejemplo.cl", null, now, "ENTREGADA", BigDecimal.valueOf(45000));
        service.ingest(delivered);
        service.ingest(delivered);
        var sales = service.sales(now.minusSeconds(60), now.plusSeconds(60));
        assertEquals(1, sales.deliveredOrders());
        assertEquals(0, BigDecimal.valueOf(45000).compareTo(sales.revenue()));
        assertEquals(2, service.audit(3L).size());
    }

    @Test
    void aceptaRangoIsoYProtegeLaConsulta() throws Exception {
        String from = Instant.now().minusSeconds(60).toString();
        String to = Instant.now().plusSeconds(60).toString();
        mvc.perform(get("/api/reports/sales").param("from", from).param("to", to))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/reports/sales").header("X-Internal-Key", "test-key")
                        .param("from", from).param("to", to))
                .andExpect(status().isOk());
    }
}
