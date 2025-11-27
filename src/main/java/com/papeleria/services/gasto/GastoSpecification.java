package com.papeleria.services.gasto;

import com.papeleria.models.Gasto;
import java.time.LocalDate;
import org.springframework.data.jpa.domain.Specification;

public class GastoSpecification {

    private GastoSpecification() {
        // Utility class
    }

    public static Specification<Gasto> descripcionContains(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("descripcion")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<Gasto> fechaBetween(LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            if (desde != null && hasta != null) {
                return cb.between(root.get("fecha"), desde, hasta);
            }
            return desde != null
                    ? cb.greaterThanOrEqualTo(root.get("fecha"), desde)
                    : cb.lessThanOrEqualTo(root.get("fecha"), hasta);
        };
    }
}
