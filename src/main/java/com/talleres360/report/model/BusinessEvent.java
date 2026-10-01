package com.talleres360.report.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "business_events", indexes = {
        @Index(name = "idx_business_events_order", columnList = "orderId"),
        @Index(name = "idx_business_events_time", columnList = "occurredAt")
})
@Getter @Setter @NoArgsConstructor
public class BusinessEvent {
    @Id @Column(length = 36) private String eventId;
    @Column(nullable = false) private Long orderId;
    @Column(nullable = false, length = 60) private String type;
    @Column(nullable = false, length = 160) private String actor;
    @Column(length = 500) private String reason;
    @Column(nullable = false) private Instant occurredAt;
    @Column(precision = 12, scale = 2) private BigDecimal total;
    @Column(length = 30) private String status;
}
