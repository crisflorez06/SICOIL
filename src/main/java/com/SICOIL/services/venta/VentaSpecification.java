package com.SICOIL.services.venta;

import com.SICOIL.models.Venta;
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
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            if (desde != null && hasta != null) {
                return cb.between(root.get("fechaRegistro"), desde, hasta);
            }
            return desde != null
                    ? cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde)
                    : cb.lessThanOrEqualTo(root.get("fechaRegistro"), hasta);
        };
    }
//
//    public static Specification<Venta> totalMin(BigDecimal min) {
//        return (root, q, cb) -> min == null ? cb.conjunction() : cb.greaterThanOrEqualTo(root.get("total"), min);
//    }
//
//    public static Specification<Venta> totalMax(BigDecimal max) {
//        return (root, q, cb) -> max == null ? cb.conjunction() : cb.lessThanOrEqualTo(root.get("total"), max);
//    }
//
//    public static Specification<Venta> totalMayorQueCero() {
//        return (root, q, cb) -> cb.greaterThan(root.get("total"), BigDecimal.ZERO);
//    }

    public static Specification<Venta> usuarioEquals(Long usuarioId) {
        return (root, q, cb) -> usuarioId == null
                ? cb.conjunction()
                : cb.equal(root.get("usuario").get("id"), usuarioId);
    }

    public static Specification<Venta> clienteEquals(Long clienteId) {
        return (root, q, cb) -> clienteId == null
                ? cb.conjunction()
                : cb.equal(root.get("cliente").get("id"), clienteId);
    }
}
