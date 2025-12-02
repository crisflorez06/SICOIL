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

    /**
     * Obtiene un listado agrupado de los pendientes en cartera, aplicando filtros por nombre
     * de cliente y por rango de fechas.
     * Debido a que la vista es agrupada por cliente, al especificar un rango de fechas se
     * devolverán únicamente los totales correspondientes a los movimientos dentro de dicho rango,
     * mientras que los saldos pendientes se calculan a partir del estado actual de la cartera.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Aplicar filtros por nombre del cliente y por saldos pendientes mayores a cero.</li>
     *   <li>Recuperar solo las carteras activas con saldo pendiente.</li>
     *   <li>Agrupar las carteras por cliente para generar una vista consolidada.</li>
     *   <li>Obtener todos los movimientos de cartera (créditos, abonos o ajustes) dentro del rango de fechas indicado.</li>
     *   <li>Calcular para cada cliente:
     *     <ul>
     *       <li>Total de créditos realizados en el rango.</li>
     *       <li>Total de abonos realizados en el rango.</li>
     *       <li>Saldo pendiente actual.</li>
     *       <li>Última fecha de actualización de la cartera.</li>
     *     </ul>
     *   </li>
     *   <li>Construir un resumen por cliente para mostrarlo en la vista agrupada.</li>
     * </ul>
     *
     * @param nombreCliente nombre del cliente para filtrar; puede ser {@code null}
     * @param desde fecha inicial del rango de consulta; puede ser {@code null}
     * @param hasta fecha final del rango de consulta; puede ser {@code null}
     * @return una lista de {@link CarteraResumenResponse} agrupada por cliente, ordenada alfabéticamente
     */
    @Transactional(readOnly = true)
    public List<CarteraResumenResponse> listarPendientes(String nombreCliente, LocalDate desde, LocalDate hasta) {
        Specification<Cartera> spec = Specification
                .where(CarteraSpecification.clienteNombreContains(nombreCliente))
                .and(CarteraSpecification.saldoMayorQueCero());

        List<Cartera> carteras = carteraRepository.findAll(spec);
        if (carteras.isEmpty()) {
            return List.of();
        }

        //agrupamos la cartera por cliente para que en la vista solo se vea el nombre con la informacion agrupada (saldo pendiente, total abono, total creditos)
        Map<Long, List<Cartera>> carterasPorCliente = agruparPorCliente(carteras);
        Set<Long> carteraIds = carteras.stream()
                .map(Cartera::getId)
                .collect(Collectors.toSet());

        //traemos todos los movimientos que se han realizado en cartera ya sea credito, abono o ajuste
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

    /**
     * Obtiene la lista de abonos realizados por un cliente dentro de un rango de fechas,
     * consultando los movimientos de cartera correspondientes y transformándolos a un
     * formato de respuesta detallada.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar el cliente mediante los métodos internos de obtención de movimientos.</li>
     *   <li>Filtrar únicamente movimientos de tipo {@link CarteraMovimientoTipo#ABONO}.</li>
     *   <li>Aplicar el rango de fechas indicado (si corresponde).</li>
     *   <li>Convertir cada movimiento a {@link CarteraAbonoDetalleResponse} mediante el mapper.</li>
     * </ul>
     *
     * @param clienteId identificador del cliente cuyos abonos se desean consultar
     * @param desde fecha inicial del rango; puede ser {@code null}
     * @param hasta fecha final del rango; puede ser {@code null}
     * @return lista de abonos detallados realizados por el cliente en el rango indicado
     */
    @Transactional(readOnly = true)
    public List<CarteraAbonoDetalleResponse> listarAbonos(Long clienteId, LocalDate desde, LocalDate hasta) {
        List<CarteraMovimiento> movimientos = obtenerMovimientos(clienteId, CarteraMovimientoTipo.ABONO, desde, hasta);
        return movimientos.stream()
                .map(carteraMovimientoMapper::toAbonoResponse)
                .toList();
    }

    /**
     * Obtiene la lista de créditos aplicados al cliente dentro de un rango de fechas,
     * consultando los movimientos de cartera correspondientes y transformándolos a un
     * formato de respuesta detallada.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar el cliente mediante los métodos internos de obtención de movimientos.</li>
     *   <li>Filbrar únicamente movimientos de tipo {@link CarteraMovimientoTipo#CREDITO}.</li>
     *   <li>Aplicar el rango de fechas indicado (si corresponde).</li>
     *   <li>Convertir cada movimiento a {@link CarteraCreditoDetalleResponse} mediante el mapper.</li>
     * </ul>
     *
     * @param clienteId identificador del cliente cuyos créditos se desean consultar
     * @param desde fecha inicial del rango; puede ser {@code null}
     * @param hasta fecha final del rango; puede ser {@code null}
     * @return lista de créditos detallados asociados al cliente en el rango indicado
     */
    @Transactional(readOnly = true)
    public List<CarteraCreditoDetalleResponse> listarCreditos(Long clienteId, LocalDate desde, LocalDate hasta) {
        List<CarteraMovimiento> movimientos = obtenerMovimientos(clienteId, CarteraMovimientoTipo.CREDITO, desde, hasta);
        return movimientos.stream()
                .map(carteraMovimientoMapper::toCreditoResponse)
                .toList();
    }



    /**
     * Registra una venta a crédito en la cartera del cliente y crea el movimiento
     * correspondiente en el historial de movimientos de cartera.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que la venta no sea nula y que esté persistida (con ID).</li>
     *   <li>Verificar que el tipo de venta sea {@link TipoVenta#CREDITO}; en caso contrario,
     *       no se genera registro en cartera.</li>
     *   <li>Validar que el total de la venta sea mayor que cero.</li>
     *   <li>Evitar la duplicidad comprobando si ya existe una cartera asociada a la venta.</li>
     *   <li>Crear el registro de {@link Cartera} con el saldo inicial igual al total de la venta.</li>
     *   <li>Registrar el movimiento inicial de tipo {@link CarteraMovimientoTipo#CREDITO}
     *       vinculado a la venta y al usuario que la generó.</li>
     * </ul>
     *
     * @param venta venta a crédito que se registrará en cartera
     * @throws IllegalArgumentException si la venta es {@code null}
     * @throws IllegalStateException si la venta no está persistida (ID nulo)
     */
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

    /**
     * Ajusta la cartera asociada a una venta que ha sido anulada, eliminando su saldo
     * vigente y registrando un movimiento de tipo {@link CarteraMovimientoTipo#AJUSTE}
     * que deja trazabilidad del cambio.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Verificar que la venta sea válida y que esté persistida.</li>
     *   <li>Buscar la cartera vinculada a la venta anulada.</li>
     *   <li>Comprobar que el saldo pendiente sea mayor a cero antes de aplicar el ajuste.</li>
     *   <li>Establecer el saldo de la cartera en cero.</li>
     *   <li>Persistir la cartera actualizada.</li>
     *   <li>Registrar un movimiento de ajuste por el monto equivalente al saldo eliminado,
     *       asociado al usuario que realizó la acción y a la observación recibida.</li>
     * </ul>
     *
     * @param venta venta anulada que origina el ajuste en cartera
     * @param usuario usuario responsable de la anulación
     * @param observacion descripción del motivo de la anulación utilizada en el movimiento de ajuste
     */
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


    /**
     * Registra un abono realizado por un cliente, aplicándolo de forma secuencial a
     * todas sus deudas pendientes en cartera (ordenadas por fecha de actualización),
     * hasta agotar el monto del abono. Cada aplicación genera un movimiento de tipo
     * {@link CarteraMovimientoTipo#ABONO} y un ajuste financiero en el módulo de capital.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar el cliente y la solicitud de abono.</li>
     *   <li>Verificar que el monto del abono sea mayor que cero.</li>
     *   <li>Recuperar todas las carteras del cliente con saldo pendiente, ordenadas
     *       por su última actualización.</li>
     *   <li>Validar que el cliente tenga deudas activas.</li>
     *   <li>Comprobar que el abono no exceda el saldo total adeudado.</li>
     *   <li>Aplicar el abono empezando por la cartera más antigua, restando el saldo
     *       y registrando cada movimiento individualmente.</li>
     *   <li>Generar una observación adecuada para cada movimiento de abono.</li>
     *   <li>Registrar en capital el impacto financiero del abono.</li>
     * </ul>
     *
     * @param clienteId identificador del cliente al que se aplicará el abono
     * @param request datos del abono, incluyendo monto y observación opcional
     * @return una lista de {@link CarteraAbonoDetalleResponse} que representa los movimientos de abono generados
     * @throws IllegalArgumentException si el cliente es inválido, si no existen deudas activas
     *                                  o si el monto del abono es incorrecto
     */
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

        //lista todos los registros de la cartera
        List<Cartera> carterasPendientes = carteraRepository
                .findByClienteIdAndSaldoGreaterThanOrderByUltimaActualizacionAsc(clienteId, 0d);

        if (carterasPendientes.isEmpty()) {
            throw new IllegalArgumentException("El cliente no tiene deudas pendientes en cartera.");
        }

        //verificamos que el saldo total de lo que debe no sea mayor a lo que se abona
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

        //aca vamos restando a cada cartera el abono, hasta que el abono vaya cubriendo la deuda de totas las carteras
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

        //por cada cliente sacamos el total de creditos y abonos que se han realizado
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
