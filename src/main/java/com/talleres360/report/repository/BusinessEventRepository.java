package com.talleres360.report.repository;

import com.talleres360.report.model.BusinessEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface BusinessEventRepository extends JpaRepository<BusinessEvent, String> {
    List<BusinessEvent> findTop200ByOrderByOccurredAtDesc();
    List<BusinessEvent> findByOrderIdOrderByOccurredAtDesc(Long orderId);
    List<BusinessEvent> findByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
            String type, Instant from, Instant to);
}
