package com.talleres360.report.repository;

import com.talleres360.report.model.BusinessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

public interface BusinessEventRepository extends JpaRepository<BusinessEvent, String> {
    List<BusinessEvent> findTop200ByOrderByOccurredAtDesc();
    List<BusinessEvent> findByOrderIdOrderByOccurredAtDesc(Long orderId);
    List<BusinessEvent> findByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            String type, Instant from, Instant to);

    interface InicioOrden {
        Long getOrderId();
        Instant getInicio();
    }

    @Query("""
            select e.orderId as orderId, min(e.occurredAt) as inicio
            from BusinessEvent e
            where e.type = 'CREADA'
              and exists (
                select d.eventId from BusinessEvent d where d.orderId = e.orderId and d.type = 'ENTREGADA'
                  and d.occurredAt >= :from and d.occurredAt < :to
              )
            group by e.orderId
            """)
    List<InicioOrden> iniciosDeOrdenesEntregadas(@Param("from") Instant from, @Param("to") Instant to);
}
