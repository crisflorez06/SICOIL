package com.SICOIL.services.venta;

import com.SICOIL.models.DetalleVenta;
import com.SICOIL.models.Producto;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDateTime;

public class DetalleVentaSpecification {

    public static Specification<DetalleVenta> productoNombreContains(String nombre) {
        return (root, query, cb) -> {
            if (nombre == null || nombre.isBlank()) {
                return cb.conjunction();
            }
            Join<DetalleVenta, Producto> productoJoin = root.join("producto");
            return cb.like(cb.lower(productoJoin.get("nombre")), "%" + nombre.toLowerCase() + "%");
        };
    }

    public static Specification<DetalleVenta> ventaFechaBetween(LocalDateTime desde, LocalDateTime hasta) {
        return (root, query, cb) -> {
            Join<DetalleVenta, Venta> ventaJoin = root.join("venta");
            if (desde == null && hasta == null) {
                return cb.conjunction();
            }
            if (desde != null && hasta != null) {
                return cb.between(ventaJoin.get("fechaRegistro"), desde, hasta);
            }
            return desde != null
                ? cb.greaterThanOrEqualTo(ventaJoin.get("fechaRegistro"), desde)
                : cb.lessThanOrEqualTo(ventaJoin.get("fechaRegistro"), hasta);
        };
    }

    public static Specification<DetalleVenta> tipoVentaEquals(TipoVenta tipoVenta) {
        return (root, query, cb) -> {
            if (tipoVenta == null) {
                return cb.conjunction();
            }
            Join<DetalleVenta, Venta> ventaJoin = root.join("venta");
            return cb.equal(ventaJoin.get("tipoVenta"), tipoVenta);
        };
    }

    public static Specification<DetalleVenta> clienteNombreContains(String nombreCliente) {
        return (root, query, cb) -> {
            if (nombreCliente == null || nombreCliente.isBlank()) {
                return cb.conjunction();
            }
            Join<DetalleVenta, Venta> ventaJoin = root.join("venta");
            Join<Venta, Cliente> clienteJoin = ventaJoin.join("cliente");
            return cb.like(cb.lower(clienteJoin.get("nombre")), "%" + nombreCliente.toLowerCase() + "%");
        };
    }

    public static Specification<DetalleVenta> usuarioNombreContains(String nombreUsuario) {
        return (root, query, cb) -> {
            if (nombreUsuario == null || nombreUsuario.isBlank()) {
                return cb.conjunction();
            }
            Join<DetalleVenta, Venta> ventaJoin = root.join("venta");
            Join<Venta, Usuario> usuarioJoin = ventaJoin.join("usuario");
            return cb.like(cb.lower(usuarioJoin.get("usuario")), "%" + nombreUsuario.toLowerCase() + "%");
        };
    }

    public static Specification<DetalleVenta> ventaActivaEquals(Boolean activa) {
        return (root, query, cb) -> {
            Join<DetalleVenta, Venta> ventaJoin = root.join("venta");
            if (activa == null) {
                return cb.isTrue(ventaJoin.get("activa"));
            }
            return activa
                    ? cb.isTrue(ventaJoin.get("activa"))
                    : cb.isFalse(ventaJoin.get("activa"));
        };
    }
}
