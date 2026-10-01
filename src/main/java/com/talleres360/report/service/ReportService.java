package com.talleres360.report.service;

import com.talleres360.report.model.BusinessEvent;
import com.talleres360.report.repository.BusinessEventRepository;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service @RequiredArgsConstructor
public class ReportService {
    private final BusinessEventRepository events;
    public record EventInput(@NotBlank String eventId, @NotNull @Positive Long orderId,
                             @NotBlank String type, @NotBlank String actor, String reason, @NotNull Instant occurredAt,
                             @NotBlank String status, @NotNull @PositiveOrZero BigDecimal total) {}
    public record SalesReport(Instant from, Instant to, long deliveredOrders, BigDecimal revenue) {}

    @Transactional
    public void ingest(EventInput input) {
        if (events.existsById(input.eventId())) return;
        BusinessEvent event = new BusinessEvent();
        event.setEventId(input.eventId());
        event.setOrderId(input.orderId());
        event.setType(input.type());
        event.setActor(input.actor());
        event.setReason(input.reason());
        event.setOccurredAt(input.occurredAt());
        event.setStatus(input.status());
        event.setTotal(input.total());
        events.save(event);
    }
    @Transactional(readOnly = true)
    public SalesReport sales(Instant from, Instant to) {
        if (!from.isBefore(to)) throw new IllegalArgumentException("El rango de fechas no es válido");
        List<BusinessEvent> delivered = events
                .findByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc("ENTREGADA", from, to);
        BigDecimal revenue = delivered.stream().map(BusinessEvent::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesReport(from, to, delivered.size(), revenue);
    }
    @Transactional(readOnly = true)
    public List<BusinessEvent> audit(Long orderId) {
        return orderId == null ? events.findTop200ByOrderByOccurredAtDesc() : events.findByOrderIdOrderByOccurredAtDesc(orderId);
    }
}
