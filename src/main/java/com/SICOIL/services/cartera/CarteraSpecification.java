package com.SICOIL.services.cartera;

import com.SICOIL.models.Cartera;
import org.springframework.data.jpa.domain.Specification;

public final class CarteraSpecification {

    private CarteraSpecification() {
    }

    public static Specification<Cartera> clienteNombreContains(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            String filtro = "%" + nombre.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("cliente").get("nombre")), filtro);
        };
    }

    public static Specification<Cartera> saldoMayorQueCero() {
        return (root, query, cb) -> cb.greaterThan(root.get("saldo"), 0d);
    }
}
