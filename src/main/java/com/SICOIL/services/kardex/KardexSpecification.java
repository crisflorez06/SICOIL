package com.SICOIL.services.kardex;

import com.SICOIL.models.Kardex;
import com.SICOIL.models.MovimientoTipo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.data.jpa.domain.Specification;

public final class KardexSpecification {

    private KardexSpecification() {
    }

    public static Specification<Kardex> productoIdEquals(Long productoId) {
        return (root, query, cb) -> {
            if (productoId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("producto").get("id"), productoId);
        };
    }

    public static Specification<Kardex> usuarioIdEquals(Long usuarioId) {
        return (root, query, cb) -> {
            if (usuarioId == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("usuario").get("id"), usuarioId);
        };
    }

    public static Specification<Kardex> productoNombreContains(String nombreProducto) {
        return (root, query, cb) -> {
            if (nombreProducto == null || nombreProducto.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + nombreProducto.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("producto").get("nombre")), pattern);
        };
    }

    public static Specification<Kardex> tipoEquals(MovimientoTipo tipo) {
        return (root, query, cb) -> {
            if (tipo == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("tipo"), tipo);
        };
    }

    public static Specification<Kardex> fechaBetween(LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
            LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(LocalTime.MAX) : null;

            if (desdeDateTime != null && hastaDateTime != null) {
                return cb.between(root.get("fechaRegistro"), desdeDateTime, hastaDateTime);
            }
            return desdeDateTime != null
                    ? cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desdeDateTime)
                    : cb.lessThanOrEqualTo(root.get("fechaRegistro"), hastaDateTime);
        };
    }

    public static Specification<Kardex> filtros(Long productoId,
                                                Long usuarioId,
                                                String nombreProducto,
                                                MovimientoTipo tipo,
                                                LocalDate desde,
                                                LocalDate hasta) {
        return Specification.where(productoIdEquals(productoId))
                .and(usuarioIdEquals(usuarioId))
                .and(productoNombreContains(nombreProducto))
                .and(tipoEquals(tipo))
                .and(fechaBetween(desde, hasta));
    }
}
