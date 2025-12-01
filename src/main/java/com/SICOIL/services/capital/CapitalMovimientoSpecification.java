package com.SICOIL.services.capital;

import com.SICOIL.models.CapitalMovimiento;
import com.SICOIL.models.CapitalOrigen;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.jpa.domain.Specification;

public final class CapitalMovimientoSpecification {

    private CapitalMovimientoSpecification() {
    }

    public static Specification<CapitalMovimiento> conFiltros(CapitalOrigen origen,
                                                              Boolean esCredito,
                                                              Long referenciaId,
                                                              String descripcion,
                                                              LocalDate desde,
                                                              LocalDate hasta) {
        return Specification
                .where(origenEquals(origen))
                .and(esCreditoEquals(esCredito))
                .and(referenciaEquals(referenciaId))
                .and(descripcionContains(descripcion))
                .and(fechaBetween(desde, hasta));
    }

    private static Specification<CapitalMovimiento> origenEquals(CapitalOrigen origen) {
        return (root, query, cb) -> {
            if (origen == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("origen"), origen);
        };
    }

    private static Specification<CapitalMovimiento> esCreditoEquals(Boolean esCredito) {
        return (root, query, cb) -> {
            if (esCredito == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("esCredito"), esCredito);
        };
    }

    private static Specification<CapitalMovimiento> referenciaEquals(Long referenciaId) {
        return (root, query, cb) -> {
            if (referenciaId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("referenciaId"), referenciaId);
        };
    }

    private static Specification<CapitalMovimiento> descripcionContains(String descripcion) {
        return (root, query, cb) -> {
            if (descripcion == null || descripcion.isBlank()) {
                return cb.conjunction();
            }
            String like = "%" + descripcion.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("descripcion")), like);
        };
    }

    private static Specification<CapitalMovimiento> fechaBetween(LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            if (desde != null && hasta != null) {
                LocalDateTime inicio = desde.atStartOfDay();
                LocalDateTime fin = hasta.atTime(LocalTime.MAX);
                return cb.between(root.get("creadoEn"), inicio, fin);
            }
            if (desde != null) {
                LocalDateTime inicio = desde.atStartOfDay();
                return cb.greaterThanOrEqualTo(root.get("creadoEn"), inicio);
            }
            LocalDateTime fin = hasta.atTime(LocalTime.MAX);
            return cb.lessThanOrEqualTo(root.get("creadoEn"), fin);
        };
    }
}
