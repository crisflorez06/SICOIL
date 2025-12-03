package com.SICOIL.services.venta;

import com.SICOIL.models.Cliente;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import jakarta.persistence.criteria.Join;
import java.time.LocalDateTime;
import org.springframework.data.jpa.domain.Specification;

public final class VentaSpecification {

    private VentaSpecification() {
    }

    public static Specification<Venta> conFiltros(TipoVenta tipoVenta,
                                                  String nombreCliente,
                                                  String nombreUsuario,
                                                  Boolean activa,
                                                  LocalDateTime desde,
                                                  LocalDateTime hasta) {
        return Specification.where(tipoVentaEquals(tipoVenta))
                .and(clienteNombreContains(nombreCliente))
                .and(usuarioNombreContains(nombreUsuario))
                .and(ventaActivaEquals(activa))
                .and(fechaBetween(desde, hasta));
    }

    private static Specification<Venta> tipoVentaEquals(TipoVenta tipoVenta) {
        return (root, query, cb) -> {
            if (tipoVenta == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("tipoVenta"), tipoVenta);
        };
    }

    private static Specification<Venta> clienteNombreContains(String nombreCliente) {
        return (root, query, cb) -> {
            if (nombreCliente == null || nombreCliente.isBlank()) {
                return cb.conjunction();
            }
            Join<Venta, Cliente> clienteJoin = root.join("cliente");
            return cb.like(cb.lower(clienteJoin.get("nombre")), "%" + nombreCliente.toLowerCase() + "%");
        };
    }

    private static Specification<Venta> usuarioNombreContains(String nombreUsuario) {
        return (root, query, cb) -> {
            if (nombreUsuario == null || nombreUsuario.isBlank()) {
                return cb.conjunction();
            }
            Join<Venta, Usuario> usuarioJoin = root.join("usuario");
            return cb.like(cb.lower(usuarioJoin.get("usuario")), "%" + nombreUsuario.toLowerCase() + "%");
        };
    }

    private static Specification<Venta> ventaActivaEquals(Boolean activa) {
        return (root, query, cb) -> {
            if (activa == null) {
                return cb.isTrue(root.get("activa"));
            }
            return activa
                    ? cb.isTrue(root.get("activa"))
                    : cb.isFalse(root.get("activa"));
        };
    }

    private static Specification<Venta> fechaBetween(LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            if (desde != null && hasta != null) {
                return cb.between(root.get("fechaRegistro"), desde, hasta);
            }
            return desde != null
                    ? cb.greaterThanOrEqualTo(root.get("fechaRegistro"), desde)
                    : cb.lessThanOrEqualTo(root.get("fechaRegistro"), hasta);
        };
    }
}
