package com.SICOIL.services.cartera;

import com.SICOIL.models.CarteraMovimiento;
import com.SICOIL.models.CarteraMovimientoTipo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;
import org.springframework.data.jpa.domain.Specification;

public final class CarteraMovimientoSpecification {

    private CarteraMovimientoSpecification() {
    }

    public static Specification<CarteraMovimiento> carteraIdIn(Set<Long> carteraIds) {
        return (root, query, cb) -> {
            if (carteraIds == null || carteraIds.isEmpty()) {
                return cb.disjunction();
            }
            return root.get("cartera").get("id").in(carteraIds);
        };
    }

    public static Specification<CarteraMovimiento> clienteIdEquals(Long clienteId) {
        return (root, query, cb) -> {
            if (clienteId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("cartera").get("cliente").get("id"), clienteId);
        };
    }

    public static Specification<CarteraMovimiento> tipoEquals(CarteraMovimientoTipo tipo) {
        return (root, query, cb) -> {
            if (tipo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("tipo"), tipo);
        };
    }

    public static Specification<CarteraMovimiento> fechaBetween(LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }

            LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
            LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

            if (desdeDateTime != null && hastaDateTime != null) {
                return cb.between(root.get("fecha"), desdeDateTime, hastaDateTime);
            }

            return desdeDateTime != null
                    ? cb.greaterThanOrEqualTo(root.get("fecha"), desdeDateTime)
                    : cb.lessThanOrEqualTo(root.get("fecha"), hastaDateTime);
        };
    }
}
