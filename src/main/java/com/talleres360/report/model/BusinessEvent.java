package com.talleres360.report.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "eventos_reporteria", indexes = {
        @Index(name = "idx_business_events_order", columnList = "solicitud_id"),
        @Index(name = "idx_business_events_time", columnList = "ocurrido_en")
})
@Getter @Setter @NoArgsConstructor
public class BusinessEvent {
    @Id @Column(name = "evento_id", length = 36) private String eventId;
    @Column(name = "solicitud_id", nullable = false) private Long orderId;
    @Column(name = "tipo", nullable = false, length = 60) private String type;
    @Column(name = "usuario_accion", nullable = false, length = 160) private String actor;
    @Column(name = "motivo", length = 500) private String reason;
    @Column(name = "ocurrido_en", nullable = false) private Instant occurredAt;
    @Column(name = "total", precision = 12, scale = 2) private BigDecimal total;
    @Column(name = "estado", length = 30) private String status;
}
