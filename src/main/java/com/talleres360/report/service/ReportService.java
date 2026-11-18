package com.talleres360.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talleres360.report.model.BusinessEvent;
import com.talleres360.report.repository.BusinessEventRepository;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service @RequiredArgsConstructor
public class ReportService {
    private final BusinessEventRepository events;
    private final ObjectMapper json;
    public record EventInput(@NotBlank String eventId, @NotNull @Positive Long orderId,
                             @NotBlank String type, @NotBlank String actor, String reason, @NotNull Instant occurredAt,
                             @NotBlank String status, @NotNull @PositiveOrZero BigDecimal total) {}
    public record SalesReport(Instant from, Instant to, long deliveredOrders, BigDecimal revenue) {}
    public record ProductoVendido(
            @NotNull @Positive Long productoId, @NotBlank @Size(max = 160) String nombre,
            @NotNull @Positive Integer cantidad) {}
    public record VentasDia(LocalDate fecha, long entregas, BigDecimal monto) {}
    public record TiempoDia(LocalDate fecha, long entregasMedidas, double promedioMinutos) {}
    public record ProductoKpi(long productoId, String nombre, long unidades) {}
    public record KpisReport(
            SalesReport resumen, List<VentasDia> ventasPorDia, List<TiempoDia> tiemposPorDia,
            List<ProductoKpi> productos, long pedidosConDetalle, long entregasMedidas,
            Double promedioMinutos) {}

    @Transactional
    public void ingest(EventInput input) {
        ingest(input, null);
    }

    @Transactional
    public void ingest(EventInput input, List<ProductoVendido> productos) {
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
        if ("ENTREGADA".equals(input.type()) && productos != null) {
            try {
                event.setProductosVentaJson(json.writeValueAsString(productos));
            } catch (JsonProcessingException error) {
                throw new IllegalStateException("No se pudo guardar el detalle de venta", error);
            }
        }
        events.save(event);
    }
    @Transactional(readOnly = true)
    public SalesReport sales(Instant from, Instant to) {
        return resumen(from, to, entregadas(from, to));
    }

    @Transactional(readOnly = true)
    public KpisReport kpis(Instant from, Instant to, ZoneId zona) {
        List<BusinessEvent> entregas = entregadas(from, to);
        Map<Long, Instant> inicios = new HashMap<>();
        events.iniciosDeOrdenesEntregadas(from, to)
                .forEach(inicio -> inicios.put(inicio.getOrderId(), inicio.getInicio()));
        Map<LocalDate, VentasDia> ventas = new TreeMap<>();
        Map<LocalDate, DoubleSummaryStatistics> tiempos = new TreeMap<>();
        Map<Long, ProductoKpi> productos = new HashMap<>();
        var tiempoTotal = new DoubleSummaryStatistics();
        long pedidosConDetalle = 0;
        for (BusinessEvent entrega : entregas) {
            LocalDate fecha = entrega.getOccurredAt().atZone(zona).toLocalDate();
            ventas.merge(fecha, new VentasDia(fecha, 1, entrega.getTotal()),
                    (dia, venta) -> new VentasDia(fecha, dia.entregas() + 1, dia.monto().add(venta.monto())));

            Instant inicio = inicios.get(entrega.getOrderId());
            if (inicio != null && !inicio.isAfter(entrega.getOccurredAt())) {
                double minutos = Duration.between(inicio, entrega.getOccurredAt()).toMillis() / 60_000.0;
                tiempoTotal.accept(minutos);
                tiempos.computeIfAbsent(fecha, key -> new DoubleSummaryStatistics()).accept(minutos);
            }

            if (entrega.getProductosVentaJson() != null) {
                pedidosConDetalle++;
                for (ProductoVendido producto : productosDeEntrega(entrega)) {
                    var kpi = new ProductoKpi(producto.productoId(), producto.nombre(), producto.cantidad());
                    productos.merge(kpi.productoId(), kpi, (acumulado, vendido) -> new ProductoKpi(
                            acumulado.productoId(), acumulado.nombre(), acumulado.unidades() + vendido.unidades()));
                }
            }
        }

        List<TiempoDia> tiemposPorDia = tiempos.entrySet().stream()
                .map(dia -> new TiempoDia(dia.getKey(), dia.getValue().getCount(), dia.getValue().getAverage()))
                .toList();
        List<ProductoKpi> productosPrincipales = productos.values().stream()
                .sorted(Comparator.comparingLong(ProductoKpi::unidades).reversed()
                        .thenComparingLong(ProductoKpi::productoId))
                .limit(10)
                .toList();
        return new KpisReport(resumen(from, to, entregas), List.copyOf(ventas.values()), tiemposPorDia,
                productosPrincipales, pedidosConDetalle, tiempoTotal.getCount(),
                tiempoTotal.getCount() == 0 ? null : tiempoTotal.getAverage());
    }

    private List<BusinessEvent> entregadas(Instant from, Instant to) {
        if (!from.isBefore(to)) throw new IllegalArgumentException("El rango de fechas no es válido");
        return events.findByTypeAndOccurredAtGreaterThanEqualAndOccurredAtLessThanOrderByOccurredAtDesc(
                "ENTREGADA", from, to);
    }

    private SalesReport resumen(Instant from, Instant to, List<BusinessEvent> delivered) {
        BigDecimal revenue = delivered.stream().map(BusinessEvent::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new SalesReport(from, to, delivered.size(), revenue);
    }

    private List<ProductoVendido> productosDeEntrega(BusinessEvent entrega) {
        try {
            return json.readValue(entrega.getProductosVentaJson(), new TypeReference<>() {});
        } catch (JsonProcessingException error) {
            throw new IllegalStateException("No se pudo leer el detalle de la entrega", error);
        }
    }
    @Transactional(readOnly = true)
    public List<BusinessEvent> audit(Long orderId) {
        return orderId == null ? events.findTop200ByOrderByOccurredAtDesc() : events.findByOrderIdOrderByOccurredAtDesc(orderId);
    }
}
