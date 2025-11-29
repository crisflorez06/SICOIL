package com.SICOIL.services.producto;

import com.SICOIL.models.Producto;
import org.springframework.data.jpa.domain.Specification;

public final class ProductoSpecification {

    private ProductoSpecification() {
    }

    public static Specification<Producto> hasNombre(String nombre) {
        return (root, query, criteriaBuilder) -> {
            if (nombre == null || nombre.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String filtro = "%" + nombre.trim().toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("nombre")), filtro);
        };
    }

}
