package com.SICOIL.services.cliente;

import com.SICOIL.models.Cliente;
import org.springframework.data.jpa.domain.Specification;

public final class ClienteSpecification {

    private ClienteSpecification() {
    }

    public static Specification<Cliente> nombreContains(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            String filtro = "%" + nombre.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("nombre")), filtro);
        };
    }
}
