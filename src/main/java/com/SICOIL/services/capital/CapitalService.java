package com.SICOIL.services.capital;

import com.SICOIL.dtos.capital.CapitalMovimientoFiltro;
import com.SICOIL.dtos.capital.CapitalMovimientoResponse;
import com.SICOIL.dtos.capital.CapitalResumenResponse;
import com.SICOIL.models.CapitalMovimiento;
import com.SICOIL.models.CapitalOrigen;
import com.SICOIL.models.Cartera;
import com.SICOIL.models.Producto;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.CapitalMovimientoRepository;
import com.SICOIL.repositories.CarteraRepository;
import com.SICOIL.services.usuario.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class CapitalService {

    private final CapitalMovimientoRepository capitalMovimientoRepository;
    private final CarteraRepository carteraRepository;
    private final UsuarioService usuarioService;

    public void registrarIngresoInventario(Producto producto,
                                           double costoUnitario,
                                           int cantidad,
                                           String referencia) {
        if (producto == null || producto.getId() == null) {
            throw new EntityNotFoundException("El producto es obligatorio para registrar el movimiento de capital.");
        }
        if (costoUnitario < 0 || cantidad <= 0) {
            throw new IllegalArgumentException("El costo unitario debe ser positivo y la cantidad mayor a cero.");
        }
        double total = costoUnitario * cantidad;
        if (total == 0) {
            log.debug("Ingreso de inventario sin costo para producto {}", producto.getId());
            return;
        }
        String descripcion = referencia != null && !referencia.isBlank()
                ? referencia
                : "Ingreso inventario producto " + producto.getNombre();
        Usuario usuario = obtenerUsuarioMovimiento();
        registrarMovimiento(
                CapitalOrigen.COMPRA,
                producto.getId(),
                -total,
                -total,
                false,
                descripcion,
                usuario
        );
    }

    public void registrarVentaContado(Venta venta) {
        validarVenta(venta);
        if (venta.getTipoVenta() != TipoVenta.CONTADO) {
            throw new IllegalArgumentException("La venta debe ser de contado para registrar este movimiento.");
        }
        double total = venta.getTotal();
        Usuario usuario = obtenerUsuarioMovimiento();
        registrarMovimiento(
                CapitalOrigen.VENTA,
                venta.getId(),
                total,
                total,
                false,
                "Venta contado #" + venta.getId(),
                usuario
        );
    }

    public void registrarVentaCredito(Venta venta) {
        validarVenta(venta);
        if (venta.getTipoVenta() != TipoVenta.CREDITO) {
            throw new IllegalArgumentException("La venta debe ser a crédito para registrar el movimiento.");
        }
        double total = venta.getTotal();
        Usuario usuario = obtenerUsuarioMovimiento();
        registrarMovimiento(
                CapitalOrigen.VENTA,
                venta.getId(),
                total,
                0d,
                true,
                "Venta crédito #" + venta.getId(),
                usuario
        );
    }

    public void registrarAbonoCartera(Cartera cartera, double monto, String descripcion) {
        if (cartera == null || cartera.getId() == null) {
            throw new IllegalArgumentException("La cartera es obligatoria para registrar el abono.");
        }
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a cero.");
        }
        Venta venta = cartera.getVenta();
        Long referenciaId = venta != null ? venta.getId() : cartera.getId();
        String detalle = (descripcion != null && !descripcion.isBlank()
                ? descripcion.trim()
                : "Abono cartera cliente " + cartera.getCliente().getNombre());
        Usuario usuario = obtenerUsuarioMovimiento();
        registrarMovimiento(
                CapitalOrigen.VENTA,
                referenciaId,
                monto,
                monto,
                false,
                detalle,
                usuario
        );
    }

    public void revertirVenta(Venta venta) {
        validarVenta(venta);
        double total = venta.getTotal();
        Usuario usuario = obtenerUsuarioMovimiento();
        if (venta.getTipoVenta() == TipoVenta.CONTADO) {
            registrarMovimiento(
                    CapitalOrigen.VENTA,
                    venta.getId(),
                    -total,
                    -total,
                    false,
                    "Reverso venta contado #" + venta.getId(),
                    usuario
            );
            return;
        }
        registrarMovimiento(
                CapitalOrigen.VENTA,
                venta.getId(),
                -total,
                0d,
                false,
                "Reverso venta crédito #" + venta.getId(),
                usuario
        );
    }

    public CapitalMovimientoResponse registrarInyeccionCapital(double monto, String descripcion) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto de la inyección debe ser mayor a cero.");
        }
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        String detalle = (descripcion != null && !descripcion.isBlank())
                ? descripcion.trim()
                : "Inyección de capital";
        CapitalMovimiento movimiento = registrarMovimiento(
                CapitalOrigen.INYECCION,
                null,
                monto,
                monto,
                false,
                detalle,
                usuario
        );
        return mapToResponse(movimiento);
    }

    @Transactional(readOnly = true)
    public Page<CapitalMovimientoResponse> obtenerMovimientos(CapitalMovimientoFiltro filtro, Pageable pageable) {
        Specification<CapitalMovimiento> spec = CapitalMovimientoSpecification.conFiltros(
                filtro != null ? filtro.getOrigen() : null,
                filtro != null ? filtro.getEsCredito() : null,
                filtro != null ? filtro.getReferenciaId() : null,
                filtro != null ? filtro.getDescripcion() : null,
                filtro != null ? filtro.getDesde() : null,
                filtro != null ? filtro.getHasta() : null
        );
        return capitalMovimientoRepository.findAll(spec, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public CapitalResumenResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        double saldoReal = defaultValue(capitalMovimientoRepository.sumMontoReal());
        double entradas = defaultValue(capitalMovimientoRepository.sumEntradas());
        double salidas = Math.abs(defaultValue(capitalMovimientoRepository.sumSalidas()));
        double pendientes = carteraRepository.findAll().stream()
                .mapToDouble(c -> c.getSaldo() != null ? c.getSaldo() : 0d)
                .sum();

        if (desde != null && hasta != null) {
            LocalDateTime inicio = desde.atStartOfDay();
            LocalDateTime fin = hasta.atTime(23, 59, 59);
            saldoReal = defaultValue(capitalMovimientoRepository.sumMontoRealBetween(inicio, fin));
        }

        return CapitalResumenResponse.builder()
                .saldoReal(saldoReal)
                .totalEntradas(entradas)
                .totalSalidas(salidas)
                .totalCreditoPendiente(pendientes)
                .totalCredito(pendientes)
                .capitalNeto(saldoReal - pendientes)
                .build();
    }

    private CapitalMovimiento registrarMovimiento(CapitalOrigen origen,
                                                  Long referenciaId,
                                                  double montoTotal,
                                                  double montoReal,
                                                  boolean esCredito,
                                                  String descripcion,
                                                  Usuario usuario) {
        CapitalMovimiento movimiento = CapitalMovimiento.builder()
                .origen(origen)
                .referenciaId(referenciaId)
                .montoTotal(montoTotal)
                .montoReal(montoReal)
                .esCredito(esCredito)
                .descripcion(descripcion)
                .usuario(usuario)
                .build();
        log.info("Registrando movimiento de capital origen={} referencia={} total={} real={}",
                origen, referenciaId, montoTotal, montoReal);
        return capitalMovimientoRepository.save(movimiento);
    }

    private CapitalMovimientoResponse mapToResponse(CapitalMovimiento movimiento) {
        if (movimiento == null) {
            return null;
        }
        return CapitalMovimientoResponse.builder()
                .id(movimiento.getId())
                .origen(movimiento.getOrigen())
                .referenciaId(movimiento.getReferenciaId())
                .montoTotal(movimiento.getMontoTotal())
                .montoReal(movimiento.getMontoReal())
                .esCredito(movimiento.getEsCredito())
                .descripcion(movimiento.getDescripcion())
                .creadoEn(movimiento.getCreadoEn())
                .usuarioId(movimiento.getUsuario() != null ? movimiento.getUsuario().getId() : null)
                .usuarioNombre(movimiento.getUsuario() != null ? movimiento.getUsuario().getUsuario() : null)
                .build();
    }

    private Usuario obtenerUsuarioMovimiento() {
        try {
            return usuarioService.obtenerUsuarioActual();
        } catch (Exception ex) {
            log.warn("No se pudo obtener el usuario actual para el movimiento de capital: {}", ex.getMessage());
            return null;
        }
    }

    private void validarVenta(Venta venta) {
        if (venta == null || venta.getId() == null) {
            throw new IllegalArgumentException("La venta es obligatoria para registrar movimientos de capital.");
        }
        if (venta.getTotal() == null || venta.getTotal() <= 0) {
            throw new IllegalArgumentException("La venta debe tener un total válido.");
        }
    }

    private double defaultValue(Double valor) {
        return valor != null ? valor : 0d;
    }
}
