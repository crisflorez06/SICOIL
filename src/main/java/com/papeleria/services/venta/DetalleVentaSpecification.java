package com.papeleria.services.venta;

import com.papeleria.models.DetalleVenta;
import com.papeleria.models.Producto;
import com.papeleria.models.Venta;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class DetalleVentaSpecification {

    public static Specification<DetalleVenta> productoNombreContains(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            Join<DetalleVenta, Producto> productoJoin = root.join("producto");
            return cb.like(cb.lower(productoJoin.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<DetalleVenta> ventaFechaBetween(LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            Join<DetalleVenta, Venta> ventaJoin = root.join("venta");
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            if (desde != null && hasta != null) {
                return cb.between(ventaJoin.get("fecha"), desde, hasta);
            }
            return desde != null
                ? cb.greaterThanOrEqualTo(ventaJoin.get("fecha"), desde)
                : cb.lessThanOrEqualTo(ventaJoin.get("fecha"), hasta);
        };
    }
}
