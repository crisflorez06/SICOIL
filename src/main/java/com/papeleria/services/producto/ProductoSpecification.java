package com.papeleria.services.producto;

import com.papeleria.models.Producto;
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

    public static Specification<Producto> hasCategoria(String categoria) {
        return (root, query, criteriaBuilder) -> {
            if (categoria == null || categoria.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String filtro = "%" + categoria.trim().toLowerCase() + "%";
            return criteriaBuilder.like(criteriaBuilder.lower(root.get("categoria")), filtro);
        };
    }

    public static Specification<Producto> hasEstado(Boolean estado) {
        return (root, query, criteriaBuilder) -> {
            if (estado == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("estado"), estado);
        };
    }
}
