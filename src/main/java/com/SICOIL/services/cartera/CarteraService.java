package com.SICOIL.services.cartera;

import com.SICOIL.dtos.cartera.CarteraAbonoDetalleResponse;
import com.SICOIL.dtos.cartera.CarteraAbonoRequest;
import com.SICOIL.dtos.cartera.CarteraCreditoDetalleResponse;
import com.SICOIL.dtos.cartera.CarteraResumenItem;
import com.SICOIL.dtos.cartera.CarteraResumenResponse;
import com.SICOIL.mappers.cartera.CarteraMapper;
import com.SICOIL.mappers.cartera.CarteraMovimientoMapper;
import com.SICOIL.models.Cartera;
import com.SICOIL.models.CarteraMovimiento;
import com.SICOIL.models.CarteraMovimientoTipo;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.CarteraMovimientoRepository;
import com.SICOIL.repositories.CarteraRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.SICOIL.services.capital.CapitalService;
import com.SICOIL.services.usuario.UsuarioService;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CarteraService {

    private final CarteraRepository carteraRepository;
    private final CarteraMovimientoRepository carteraMovimientoRepository;
    private final CarteraMapper carteraMapper;
    private final CarteraMovimientoMapper carteraMovimientoMapper;
    private final UsuarioService usuarioService;
    private final CapitalService capitalService;

    public void registrarVentaEnCartera(Venta venta) {
        if (venta == null) {
            throw new IllegalArgumentException("La venta a registrar en cartera es obligatoria.");
        }

        if (venta.getTipoVenta() != TipoVenta.CREDITO) {
            log.debug("Venta {} no requiere cartera por ser {}", venta.getId(), venta.getTipoVenta());
            return;
        }

        if (venta.getId() == null) {
            throw new IllegalStateException("La venta debe estar persistida antes de crear la cartera.");
        }

        Double totalVenta = venta.getTotal();
        if (totalVenta == null || totalVenta <= 0) {
            log.warn("Venta {} tiene total inválido para cartera: {}", venta.getId(), totalVenta);
            return;
        }

        if (carteraRepository.existsByVentaId(venta.getId())) {
            log.warn("La venta {} ya tiene un registro de cartera, se omite creación.", venta.getId());
            return;
        }

        Cartera cartera = Cartera.builder()
                .cliente(venta.getCliente())
                .venta(venta)
                .saldo(totalVenta)
                .build();

        Cartera guardada = carteraRepository.save(cartera);
        registrarMovimiento(guardada, CarteraMovimientoTipo.CREDITO, totalVenta, venta.getUsuario(),
                "Registro de venta a crédito");
        log.info("Cartera creada para venta {} con saldo {}", venta.getId(), totalVenta);
    }

    public void ajustarPorAnulacion(Venta venta, Usuario usuario, String observacion) {
        if (venta == null || venta.getId() == null) {
            return;
        }

        carteraRepository.findByVentaId(venta.getId()).ifPresent(cartera -> {
            double saldoAnterior = cartera.getSaldo() != null ? cartera.getSaldo() : 0;
            if (saldoAnterior <= 0) {
                return;
            }
            cartera.setSaldo(0d);
            carteraRepository.save(cartera);
            registrarMovimiento(cartera, CarteraMovimientoTipo.AJUSTE, saldoAnterior, usuario, observacion);
            log.info("Cartera ajustada a 0 por anulación de la venta {}", venta.getId());
        });
    }

    @Transactional(readOnly = true)
    public List<CarteraAbonoDetalleResponse> listarAbonos(Long clienteId, LocalDate desde, LocalDate hasta) {
        List<CarteraMovimiento> movimientos = obtenerMovimientos(clienteId, CarteraMovimientoTipo.ABONO, desde, hasta);
        return movimientos.stream()
                .map(carteraMovimientoMapper::toAbonoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CarteraCreditoDetalleResponse> listarCreditos(Long clienteId, LocalDate desde, LocalDate hasta) {
        List<CarteraMovimiento> movimientos = obtenerMovimientos(clienteId, CarteraMovimientoTipo.CREDITO, desde, hasta);
        return movimientos.stream()
                .map(carteraMovimientoMapper::toCreditoResponse)
                .toList();
    }

    public List<CarteraAbonoDetalleResponse> registrarAbono(Long clienteId, CarteraAbonoRequest request) {
        if (clienteId == null) {
            throw new IllegalArgumentException("Debe indicar el cliente para registrar el abono.");
        }
        if (request == null) {
            throw new IllegalArgumentException("La solicitud de abono es obligatoria.");
        }

        double monto = request.getMonto();
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto del abono debe ser mayor a cero.");
        }

        List<Cartera> carterasPendientes = carteraRepository
                .findByClienteIdAndSaldoGreaterThanOrderByUltimaActualizacionAsc(clienteId, 0d);

        if (carterasPendientes.isEmpty()) {
            throw new IllegalArgumentException("El cliente no tiene deudas pendientes en cartera.");
        }

        double saldoTotal = carterasPendientes.stream()
                .mapToDouble(c -> c.getSaldo() != null ? c.getSaldo() : 0)
                .sum();

        if (monto > saldoTotal) {
            throw new IllegalArgumentException("El abono excede el saldo pendiente total del cliente.");
        }

        Usuario usuarioActual = usuarioService.obtenerUsuarioActual();
        String observacion = request.getObservacion() != null ? request.getObservacion().trim() : null;

        double restante = monto;
        List<CarteraAbonoDetalleResponse> movimientosRegistrados = new ArrayList<>();

        for (Cartera cartera : carterasPendientes) {
            if (restante <= 0) {
                break;
            }

            double saldoActual = cartera.getSaldo() != null ? cartera.getSaldo() : 0;
            if (saldoActual <= 0) {
                continue;
            }

            double aplicado = Math.min(restante, saldoActual);
            cartera.setSaldo(saldoActual - aplicado);
            carteraRepository.save(cartera);

            String observacionMovimiento = construirObservacionAbono(observacion, cartera);
            CarteraMovimiento movimiento = registrarMovimiento(
                    cartera,
                    CarteraMovimientoTipo.ABONO,
                    aplicado,
                    usuarioActual,
                    observacionMovimiento
            );
            movimientosRegistrados.add(carteraMovimientoMapper.toAbonoResponse(movimiento));
            capitalService.registrarAbonoCartera(cartera, aplicado, observacionMovimiento);
            restante -= aplicado;
        }

        return movimientosRegistrados;
    }

    @Transactional(readOnly = true)
    public List<CarteraResumenResponse> listarPendientes(String nombreCliente, LocalDate desde, LocalDate hasta) {
        Specification<Cartera> spec = Specification
                .where(CarteraSpecification.clienteNombreContains(nombreCliente))
                .and(CarteraSpecification.saldoMayorQueCero());

        List<Cartera> carteras = carteraRepository.findAll(spec);
        if (carteras.isEmpty()) {
            return List.of();
        }

        Map<Long, List<Cartera>> carterasPorCliente = agruparPorCliente(carteras);
        Set<Long> carteraIds = carteras.stream()
                .map(Cartera::getId)
                .collect(Collectors.toSet());

        List<CarteraMovimiento> movimientos = carteraMovimientoRepository.findAll(
                Specification.where(CarteraMovimientoSpecification.carteraIdIn(carteraIds))
                        .and(CarteraMovimientoSpecification.fechaBetween(desde, hasta))
        );

        Map<Long, TotalesMovimiento> totalesPorCliente = calcularTotalesPorCliente(movimientos);

        return carterasPorCliente.entrySet().stream()
                .map(entry -> construirResumenItem(entry.getKey(), entry.getValue(), totalesPorCliente))
                .map(carteraMapper::toResumenResponse)
                .sorted(Comparator.comparing(CarteraResumenResponse::getClienteNombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private Map<Long, List<Cartera>> agruparPorCliente(List<Cartera> carteras) {
        return carteras.stream()
                .collect(Collectors.groupingBy(c -> c.getCliente().getId()));
    }

    private List<CarteraMovimiento> obtenerMovimientos(Long clienteId,
                                                       CarteraMovimientoTipo tipo,
                                                       LocalDate desde,
                                                       LocalDate hasta) {
        if (clienteId == null) {
            throw new IllegalArgumentException("Debe indicar el cliente para consultar la cartera.");
        }

        Specification<CarteraMovimiento> spec = Specification
                .where(CarteraMovimientoSpecification.clienteIdEquals(clienteId))
                .and(CarteraMovimientoSpecification.tipoEquals(tipo))
                .and(CarteraMovimientoSpecification.fechaBetween(desde, hasta));

        return carteraMovimientoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fecha"));
    }

    private CarteraResumenItem construirResumenItem(Long clienteId,
                                                    List<Cartera> carterasCliente,
                                                    Map<Long, TotalesMovimiento> totalesPorCliente) {

        double saldoPendiente = carterasCliente.stream()
                .mapToDouble(c -> c.getSaldo() != null ? c.getSaldo() : 0)
                .sum();

        LocalDateTime ultimaActualizacion = carterasCliente.stream()
                .map(Cartera::getUltimaActualizacion)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        TotalesMovimiento totalesMovimiento = totalesPorCliente.getOrDefault(clienteId, TotalesMovimiento.vacio());

        CarteraResumenItem item = new CarteraResumenItem();
        item.setClienteId(clienteId);
        item.setClienteNombre(carterasCliente.get(0).getCliente().getNombre());
        item.setSaldoPendiente(saldoPendiente);
        item.setTotalAbonos(totalesMovimiento.totalAbonos());
        item.setTotalCreditos(totalesMovimiento.totalCreditos());
        item.setUltimaActualizacion(ultimaActualizacion);
        return item;
    }

    private String construirObservacionAbono(String observacionBase, Cartera cartera) {
        StringBuilder builder = new StringBuilder();
        if (observacionBase != null && !observacionBase.isBlank()) {
            builder.append(observacionBase.trim());
        } else {
            builder.append("Abono registrado manualmente");
        }
        if (cartera.getVenta() != null) {
            builder.append(" - Venta ").append(cartera.getVenta().getId());
        }
        return builder.toString();
    }

    private Map<Long, TotalesMovimiento> calcularTotalesPorCliente(List<CarteraMovimiento> movimientos) {
        Map<Long, TotalesMovimiento> totales = new HashMap<>();

        for (CarteraMovimiento movimiento : movimientos) {
            if (movimiento.getCartera() == null || movimiento.getCartera().getCliente() == null) {
                continue;
            }
            Long clienteId = movimiento.getCartera().getCliente().getId();
            TotalesMovimiento acumulado = totales.computeIfAbsent(clienteId, id -> new TotalesMovimiento());
            double monto = movimiento.getMonto() != null ? movimiento.getMonto() : 0;

            if (movimiento.getTipo() == CarteraMovimientoTipo.CREDITO) {
                acumulado.agregarCredito(monto);
            } else if (movimiento.getTipo() == CarteraMovimientoTipo.ABONO) {
                acumulado.agregarAbono(monto);
            }
        }
        return totales;
    }

    private CarteraMovimiento registrarMovimiento(Cartera cartera,
                                                  CarteraMovimientoTipo tipo,
                                                  Double monto,
                                                  Usuario usuario,
                                                  String observacion) {

        if (monto == null || monto <= 0) {
            throw new IllegalArgumentException("El monto del movimiento debe ser mayor a cero.");
        }

        CarteraMovimiento movimiento = CarteraMovimiento.builder()
                .cartera(cartera)
                .tipo(tipo)
                .monto(monto)
                .usuario(usuario)
                .observacion(observacion)
                .build();

        return carteraMovimientoRepository.save(movimiento);
    }

    private static final class TotalesMovimiento {
        private double totalCreditos;
        private double totalAbonos;

        private static TotalesMovimiento vacio() {
            return new TotalesMovimiento();
        }

        private void agregarCredito(double monto) {
            totalCreditos += monto;
        }

        private void agregarAbono(double monto) {
            totalAbonos += monto;
        }

        private double totalCreditos() {
            return totalCreditos;
        }

        private double totalAbonos() {
            return totalAbonos;
        }
    }
}
