package com.SICOIL.services.capital;

import com.SICOIL.dtos.capital.CapitalMovimientoFiltro;
import com.SICOIL.dtos.capital.CapitalClienteResumen;
import com.SICOIL.dtos.capital.CapitalMovimientoResponse;
import com.SICOIL.dtos.capital.CapitalProductoResumen;
import com.SICOIL.dtos.capital.CapitalResumenResponse;
import com.SICOIL.dtos.capital.CapitalVentaMensual;
import com.SICOIL.mappers.capital.CapitalMovimientoMapper;
import com.SICOIL.models.CapitalMovimiento;
import com.SICOIL.models.CapitalOrigen;
import com.SICOIL.models.Cartera;
import com.SICOIL.models.Producto;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.CapitalMovimientoRepository;
import com.SICOIL.repositories.CarteraRepository;
import com.SICOIL.repositories.CarteraMovimientoRepository;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.repositories.VentaRepository;
import com.SICOIL.services.usuario.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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
    private final CarteraMovimientoRepository carteraMovimientoRepository;
    private final VentaRepository ventaRepository;
    private final UsuarioService usuarioService;
    private final CapitalMovimientoMapper capitalMovimientoMapper;
    private final ProductoRepository productoRepository;


    /**
     * Registra en el módulo de capital el impacto financiero generado por un ingreso
     * de inventario, ya sea por la creación de un nuevo producto o por la llegada de
     * unidades adicionales.
     * El registro se realiza como un movimiento de origen {@link CapitalOrigen#COMPRA},
     * afectando la salida de capital en función del costo unitario y la cantidad ingresada.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que el producto sea válido y esté persistido.</li>
     *   <li>Verificar que el costo unitario sea positivo y la cantidad mayor que cero.</li>
     *   <li>Calcular el valor total de la operación (costo unitario × cantidad).</li>
     *   <li>Omitir movimientos cuyo total sea cero, registrándolo únicamente en logs.</li>
     *   <li>Construir la descripción del movimiento a partir de la referencia o de un mensaje predeterminado.</li>
     *   <li>Registrar el movimiento de capital como una salida (valor negativo) utilizando
     *       el usuario responsable del movimiento.</li>
     * </ul>
     *
     * @param producto producto asociado al ingreso de inventario
     * @param costoUnitario costo por unidad del producto ingresado
     * @param cantidad cantidad de unidades ingresadas al inventario
     * @param referencia descripción opcional del movimiento; si no se envía, se genera una por defecto
     * @throws EntityNotFoundException si el producto es nulo o no está persistido
     * @throws IllegalArgumentException si el costo unitario es negativo o la cantidad no es válida
     */
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
                false,
                descripcion,
                usuario,
                producto.getFechaRegistro()
        );
    }

    /**
     * Registra en el módulo de capital el ingreso correspondiente a una venta de contado.
     * El movimiento se almacena como un origen {@link CapitalOrigen#VENTA} y representa
     * un aumento inmediato en el capital disponible.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que la venta sea válida y esté persistida.</li>
     *   <li>Verificar que el tipo de venta sea {@link TipoVenta#CONTADO}.</li>
     *   <li>Obtener el total de la venta.</li>
     *   <li>Registrar el movimiento de capital como un ingreso directo.</li>
     * </ul>
     *
     * @param venta la venta de contado cuyo ingreso se registrará en capital
     * @throws IllegalArgumentException si la venta no es de tipo contado
     */
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
                false,
                "Venta contado #" + venta.getId(),
                usuario,
                venta.getFechaRegistro()
        );
    }

    /**
     * Registra en el módulo de capital el ingreso correspondiente a una venta a crédito.
     * El movimiento se almacena como un origen {@link CapitalOrigen#VENTA} y se marca
     * como diferido, ya que el pago no es inmediato, permitiendo un control contable
     * diferenciado de los ingresos por crédito.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que la venta sea válida y esté persistida.</li>
     *   <li>Verificar que el tipo de venta sea {@link TipoVenta#CREDITO}.</li>
     *   <li>Obtener el total de la venta.</li>
     *   <li>Registrar el movimiento como ingreso diferido en capital.</li>
     * </ul>
     *
     * @param venta la venta a crédito cuyo ingreso diferido se registrará en capital
     * @throws IllegalArgumentException si la venta no es de tipo crédito
     */
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
                true,
                "Venta crédito #" + venta.getId(),
                usuario,
                venta.getFechaRegistro()
        );
    }

    /**
     * Registra en el módulo de capital el ingreso correspondiente a un abono realizado
     * sobre una cartera pendiente.
     * El movimiento se almacena como un origen {@link CapitalOrigen#ABONO}, permitiendo
     * diferenciar los pagos posteriores a una venta de los ingresos directos por venta.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que la cartera exista y esté persistida.</li>
     *   <li>Verificar que el monto del abono sea mayor que cero.</li>
     *   <li>Determinar el identificador de referencia del movimiento:
     *       <ul>
     *         <li>Si la cartera está asociada a una venta → se usa el ID de la venta.</li>
     *         <li>Si no → se usa el propio ID de la cartera.</li>
     *       </ul>
     *   </li>
     *   <li>Construir una descripción adecuada del movimiento (usando la observación enviada
     *       o una generada automáticamente).</li>
     *   <li>Registrar el movimiento en capital como un ingreso directo, asociado al usuario
     *       que realizó la operación.</li>
     * </ul>
     *
     * @param cartera la cartera a la que corresponde el abono
     * @param monto monto del abono aplicado a la deuda
     * @param descripcion texto descriptivo del movimiento; puede ser {@code null} o vacío
     * @throws IllegalArgumentException si la cartera no es válida o si el monto es menor o igual a cero
     */
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
                : cartera.getCliente().getNombre());
        Usuario usuario = obtenerUsuarioMovimiento();
        registrarMovimiento(
                CapitalOrigen.ABONO,
                referenciaId,
                monto,
                false,
                detalle,
                usuario,
                LocalDateTime.now()
        );
    }

    /**
     * Registra un movimiento negativo en capital para revertir el impacto de un abono
     * previamente aplicado a una cartera. Se utiliza principalmente cuando se elimina
     * un abono erróneo y se requiere devolver el saldo pendiente y el flujo financiero.
     *
     * @param cartera cartera asociada al abono a revertir
     * @param monto monto del abono que se está eliminando
     * @param descripcion motivo detallado de la eliminación; puede ser {@code null}
     */
    public void revertirAbonoCartera(Cartera cartera, double monto, String descripcion) {
        if (cartera == null || cartera.getId() == null) {
            throw new IllegalArgumentException("La cartera es obligatoria para revertir el abono.");
        }
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto del abono a revertir debe ser mayor a cero.");
        }
        Venta venta = cartera.getVenta();
        Long referenciaId = venta != null ? venta.getId() : cartera.getId();
        String detalle = (descripcion != null && !descripcion.isBlank())
                ? descripcion.trim()
                : "Reverso abono cartera cliente " + cartera.getCliente().getNombre();
        Usuario usuario = obtenerUsuarioMovimiento();
        registrarMovimiento(
                CapitalOrigen.ABONO,
                referenciaId,
                -monto,
                false,
                detalle,
                usuario,
                LocalDateTime.now()
        );
    }

    /**
     * Revierte el impacto financiero de una venta previamente registrada en el módulo
     * de capital, generando un movimiento negativo que anula el ingreso original.
     * El comportamiento varía según el tipo de venta (contado o crédito), manteniendo la
     * coherencia entre ingresos inmediatos y diferidos.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que la venta sea válida y esté persistida.</li>
     *   <li>Obtener el total de la venta y al usuario que realiza la reversión.</li>
     *   <li>Registrar un movimiento negativo que anula el ingreso previo:
     *     <ul>
     *       <li>Si la venta fue de contado → reverso inmediato (no diferido).</li>
     *       <li>Si la venta fue a crédito → reverso marcado como diferido.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param venta venta cuyo movimiento financiero será revertido
     */
    public void revertirVenta(Venta venta) {
        validarVenta(venta);
        double total = venta.getTotal();
        Usuario usuario = obtenerUsuarioMovimiento();
        if (venta.getTipoVenta() == TipoVenta.CONTADO) {
            registrarMovimiento(
                    CapitalOrigen.VENTA,
                    venta.getId(),
                    -total,
                    false,
                    "Reverso venta contado #" + venta.getId(),
                    usuario,
                    LocalDateTime.now()
            );
            return;
        }
        registrarMovimiento(
                CapitalOrigen.VENTA,
                venta.getId(),
                -total,
                true,
                "Reverso venta crédito #" + venta.getId(),
                usuario,
                LocalDateTime.now()
        );
    }

    /**
     * Registra una inyección de capital en el sistema financiero, creando un movimiento
     * positivo que incrementa directamente el capital disponible.
     * Este tipo de operación no está asociada a una venta ni a un producto, por lo que
     * no utiliza identificadores de referencia externos.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que el monto de la inyección sea mayor que cero.</li>
     *   <li>Obtener el usuario que realiza la operación.</li>
     *   <li>Construir una descripción adecuada a partir del texto recibido o una predeterminada.</li>
     *   <li>Registrar el movimiento como un ingreso de origen {@link CapitalOrigen#INYECCION}.</li>
     *   <li>Convertir el movimiento almacenado a {@link CapitalMovimientoResponse} mediante el mapper.</li>
     * </ul>
     *
     * @param monto monto de capital que será inyectado al sistema
     * @param descripcion texto descriptivo del movimiento; puede ser {@code null} o vacío
     * @return un {@link CapitalMovimientoResponse} con la información del movimiento registrado
     * @throws IllegalArgumentException si el monto es menor o igual a cero
     */
    public CapitalMovimientoResponse registrarInyeccionCapital(double monto, String descripcion, LocalDateTime fecha) {
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
                false,
                detalle,
                usuario,
                fecha
        );
        return capitalMovimientoMapper.toResponse(movimiento);
    }

    public CapitalMovimientoResponse registrarRetiroCapital(double monto, String descripcion, LocalDateTime fecha) {
        if (monto <= 0) {
            throw new IllegalArgumentException("El monto del retiro de ganancia debe ser mayor a cero.");
        }
        Usuario usuario = usuarioService.obtenerUsuarioActual();
        String detalle = (descripcion != null && !descripcion.isBlank())
                ? descripcion.trim()
                : "Retiro de ganancias";
        CapitalMovimiento movimiento = registrarMovimiento(
                CapitalOrigen.RETIROGANANCIA,
                null,
                -monto,
                false,
                detalle,
                usuario,
                fecha
        );
        return capitalMovimientoMapper.toResponse(movimiento);
    }

    /**
     * Obtiene una página de movimientos de capital aplicando filtros opcionales por origen, crédito,
     * referencia, descripción y rango de fechas. Los movimientos resultantes se convierten en DTOs
     * mediante {@link CapitalMovimientoMapper}.
     *
     * @param filtro   criterios a aplicar; puede ser {@code null} para traer todo
     * @param pageable configuración de paginación y ordenamiento
     * @return página de {@link CapitalMovimientoResponse} que cumplen con los filtros solicitados
     */
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
                .map(capitalMovimientoMapper::toResponse);
    }

    /**
     * Consolida un resumen financiero con saldos, entradas, salidas, créditos pendientes y ganancias.
     * Si se especifica un rango de fechas, el saldo real se recalcula considerando únicamente los
     * movimientos líquidos dentro de ese intervalo; las otras métricas siempre se basan en el estado
     * completo del sistema.
     *
     * @param desde fecha inicial (inclusive) para filtrar el saldo real; opcional
     * @param hasta fecha final (inclusive) para filtrar el saldo real; opcional
     * @return instancia de {@link CapitalResumenResponse} con los totales calculados
     */
    @Transactional(readOnly = true)
    public CapitalResumenResponse obtenerResumen(LocalDate desde, LocalDate hasta) {
        LocalDateTime inicio = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime fin = hasta != null ? hasta.atTime(23, 59, 59) : null;
        boolean filtrarPorRango = inicio != null || fin != null;

        double saldoReal = filtrarPorRango
                ? defaultValue(capitalMovimientoRepository.sumMontoRealBetween(inicio, fin))
                : defaultValue(capitalMovimientoRepository.sumMontoReal());
        double entradas = filtrarPorRango
                ? defaultValue(capitalMovimientoRepository.sumEntradasBetween(inicio, fin))
                : defaultValue(capitalMovimientoRepository.sumEntradas());
        double totalCompras = filtrarPorRango
                ? defaultValue(capitalMovimientoRepository.sumComprasBetween(inicio, fin))
                : defaultValue(capitalMovimientoRepository.sumCompras());
        double salidas = Math.abs(totalCompras);
        double pendientes = carteraRepository.findAll().stream()
                .mapToDouble(c -> c.getSaldo() != null ? c.getSaldo() : 0d)
                .sum();
        double totalInventario = defaultValue(productoRepository.sumValorInventario());
        double ganancias = defaultValue(ventaRepository.sumGananciaBetween(inicio, fin));
        double totalAbonos = defaultValue(carteraMovimientoRepository.sumAbonosBetween(inicio, fin));
        double totalCreditosOtorgados = defaultValue(carteraMovimientoRepository.sumCreditosBetween(inicio, fin));
        double totalUnidadesVendidas = defaultValue(ventaRepository.sumCantidadVendida(inicio, fin));
        double totalCajasVendidas = defaultValue(ventaRepository.sumCajasVendidas(inicio, fin));
        double totalVentasPeriodo = defaultValue(ventaRepository.sumTotalVentas(inicio, fin));
        List<CapitalProductoResumen> topProductos = construirTopProductos(inicio, fin, totalUnidadesVendidas);
        List<CapitalClienteResumen> topClientes = construirTopClientes(inicio, fin, totalVentasPeriodo);
        List<CapitalVentaMensual> ventasMensuales = construirSerieVentasMensuales();

        return CapitalResumenResponse.builder()
                .saldoReal(saldoReal)
                .totalEntradas(entradas)
                .totalSalidas(salidas)
                .totalCreditoPendiente(pendientes)
                .totalCredito(totalCreditosOtorgados)
                .capitalNeto(saldoReal + pendientes + totalInventario)
                .totalGanancias(ganancias)
                .totalAbonos(totalAbonos)
                .totalInventario(totalInventario)
                .totalUnidadesVendidas(totalUnidadesVendidas)
                .totalCajasVendidas(totalCajasVendidas)
                .topProductos(topProductos)
                .topClientes(topClientes)
                .ventasMensuales(ventasMensuales)
                .build();
    }

    private List<CapitalProductoResumen> construirTopProductos(LocalDateTime inicio,
                                                               LocalDateTime fin,
                                                               double totalUnidadesVendidas) {
        Pageable limite = PageRequest.of(0, 5);
        List<Object[]> resultados = ventaRepository.findTopSellingProducts(inicio, fin, limite);
        return resultados.stream()
                .map(registro -> {
                    Long productoId = registro[0] != null ? (Long) registro[0] : null;
                    String nombre = registro[1] != null ? registro[1].toString() : "Producto";
                    long cantidad = registro[2] instanceof Number ? ((Number) registro[2]).longValue() : 0L;
                    double total = registro[3] instanceof Number ? ((Number) registro[3]).doubleValue() : 0d;
                    double participacion = totalUnidadesVendidas > 0
                            ? (cantidad / totalUnidadesVendidas) * 100
                            : 0;
                    return CapitalProductoResumen.builder()
                            .productoId(productoId)
                            .productoNombre(nombre)
                            .cantidadVendida(cantidad)
                            .totalVendido(total)
                            .participacionPorcentaje(participacion)
                            .build();
                })
                .toList();
    }

    private List<CapitalClienteResumen> construirTopClientes(LocalDateTime inicio,
                                                             LocalDateTime fin,
                                                             double totalVentasPeriodo) {
        Pageable limite = PageRequest.of(0, 5);
        List<Object[]> resultados = ventaRepository.findTopClients(inicio, fin, limite);
        return resultados.stream()
                .map(registro -> {
                    Long clienteId = registro[0] != null ? (Long) registro[0] : null;
                    String nombre = registro[1] != null ? registro[1].toString() : "Cliente";
                    long cantidadVentas = registro[2] instanceof Number ? ((Number) registro[2]).longValue() : 0L;
                    double monto = registro[3] instanceof Number ? ((Number) registro[3]).doubleValue() : 0d;
                    double participacion = totalVentasPeriodo > 0
                            ? (monto / totalVentasPeriodo) * 100
                            : 0;
                    return CapitalClienteResumen.builder()
                            .clienteId(clienteId)
                            .clienteNombre(nombre)
                            .totalVentas(cantidadVentas)
                            .montoComprado(monto)
                            .participacionPorcentaje(participacion)
                            .build();
                })
                .toList();
    }

    private List<CapitalVentaMensual> construirSerieVentasMensuales() {
        LocalDate primerMes = LocalDate.now().minusMonths(5).withDayOfMonth(1);
        LocalDateTime inicio = primerMes.atStartOfDay();
        List<Object[]> registros = ventaRepository.sumVentasMensualesDesde(inicio);
        Map<YearMonth, Double> totalesPorMes = new HashMap<>();

        for (Object[] registro : registros) {
            int anio = registro[0] instanceof Number ? ((Number) registro[0]).intValue() : primerMes.getYear();
            int mes = registro[1] instanceof Number ? ((Number) registro[1]).intValue() : primerMes.getMonthValue();
            double total = registro[2] instanceof Number ? ((Number) registro[2]).doubleValue() : 0d;
            YearMonth clave = YearMonth.of(anio, mes);
            totalesPorMes.put(clave, total);
        }

        List<CapitalVentaMensual> serie = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            LocalDate fechaMes = primerMes.plusMonths(i);
            YearMonth yearMonth = YearMonth.from(fechaMes);
            double total = totalesPorMes.getOrDefault(yearMonth, 0d);
            serie.add(CapitalVentaMensual.builder()
                    .mes(yearMonth.toString())
                    .total(total)
                    .build());
        }
        return serie;
    }

    private CapitalMovimiento registrarMovimiento(CapitalOrigen origen,
                                                  Long referenciaId,
                                                  double monto,
                                                  boolean esCredito,
                                                  String descripcion,
                                                  Usuario usuario,
                                                  LocalDateTime fecha) {
        CapitalMovimiento movimiento = CapitalMovimiento.builder()
                .origen(origen)
                .referenciaId(referenciaId)
                .monto(monto)
                .esCredito(esCredito)
                .descripcion(descripcion)
                .usuario(usuario)
                .creadoEn(fecha)
                .build();
        log.info("Registrando movimiento de capital origen={} referencia={} monto={} esCredito={}",
                origen, referenciaId, monto, esCredito);
        return capitalMovimientoRepository.save(movimiento);
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

    private double defaultValue(Number valor) {
        return valor != null ? valor.doubleValue() : 0d;
    }
}
