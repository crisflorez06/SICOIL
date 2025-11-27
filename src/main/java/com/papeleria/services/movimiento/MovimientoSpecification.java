package com.papeleria.services.movimiento;

import com.papeleria.models.Movimiento;
import com.papeleria.models.MovimientoTipo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.jpa.domain.Specification;

public final class MovimientoSpecification {

    private MovimientoSpecification() {
    }

    public static Specification<Movimiento> productoIdEquals(Long productoId) {
        return (root, query, cb) -> {
            if (productoId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("producto").get("id"), productoId);
        };
    }

    public static Specification<Movimiento> tipoEquals(MovimientoTipo tipo) {
        return (root, query, cb) -> {
            if (tipo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("tipo"), tipo);
        };
    }

    public static Specification<Movimiento> fechaBetween(LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
            LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

            if (desdeDateTime != null && hastaDateTime != null) {
                return cb.between(root.get("fechaMovimiento"), desdeDateTime, hastaDateTime);
            }
            return desdeDateTime != null
                    ? cb.greaterThanOrEqualTo(root.get("fechaMovimiento"), desdeDateTime)
                    : cb.lessThanOrEqualTo(root.get("fechaMovimiento"), hastaDateTime);
        };
    }

    public static Specification<Movimiento> cantidadDistintaDeCero() {
        return (root, query, cb) -> cb.notEqual(root.get("cantidad"), 0);
    }
}
