package com.papeleria.services.venta;

import com.papeleria.models.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public class VentaSpecification {

    public static Specification<Venta> metodoPagoEquals(String metodoPago) {
        return (root, q, cb) -> metodoPago == null || metodoPago.isBlank()
                ? cb.conjunction()
                : cb.equal(root.get("metodoPago"), metodoPago);
    }

    public static Specification<Venta> fechaBetween(LocalDateTime desde, LocalDateTime hasta) {
        return (root, q, cb) -> {
            if (desde == null && hasta == null) return cb.conjunction();
            if (desde != null && hasta != null) return cb.between(root.get("fecha"), desde, hasta);
            return desde != null ? cb.greaterThanOrEqualTo(root.get("fecha"), desde)
                    : cb.lessThanOrEqualTo(root.get("fecha"), hasta);
        };
    }

    public static Specification<Venta> totalMin(BigDecimal min) {
        return (root, q, cb) -> min == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("total"), min);
    }

    public static Specification<Venta> totalMax(BigDecimal max) {
        return (root, q, cb) -> max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("total"), max);
    }

    public static Specification<Venta> totalMayorQueCero() {
        return (root, q, cb) -> cb.greaterThan(root.get("total"), BigDecimal.ZERO);
    }
}
