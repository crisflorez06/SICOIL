package com.SICOIL.services.venta;

import com.SICOIL.dtos.venta.PaginaVentaResponse;
import com.SICOIL.dtos.venta.VentaAnulacionRequest;
import com.SICOIL.dtos.venta.VentaListadoResponse;
import com.SICOIL.dtos.venta.VentaRequest;
import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.mappers.venta.VentaMapper;
import com.SICOIL.models.*;
import com.SICOIL.repositories.VentaRepository;
import com.SICOIL.services.InventarioService;
import com.SICOIL.services.capital.CapitalService;
import com.SICOIL.services.cartera.CarteraService;
import com.SICOIL.services.cliente.ClienteService;
import com.SICOIL.services.producto.ProductoService;
import com.SICOIL.services.usuario.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoService productoService;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final VentaMapper ventaMapper;
    private final InventarioService inventarioService;
    private final CarteraService carteraService;
    private final CapitalService capitalService;

    /**
     * Recupera una lista paginada de ventas aplicando múltiples filtros opcionales,
     * agrupando sus detalles para facilitar el consumo en el frontend.
     *
     * <p><b>Comportamiento respecto a ventas anuladas:</b><br>
     * - Si {@code activa} es {@code true}, se devuelven únicamente ventas activas.<br>
     * - Si {@code activa} es {@code false}, se devuelven únicamente ventas anuladas.<br>
     * - Si {@code activa} es {@code null}, también se devuelven únicamente ventas activas,
     *   ya que las ventas anuladas solo se incluyen cuando se solicitan explícitamente.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Conversión del tipo de venta proporcionado como cadena a {@link TipoVenta}.</li>
     *   <li>Construcción dinámica de una {@link Specification} que combina los filtros solicitados.</li>
     *   <li>Ejecución de la consulta paginada sobre el repositorio de ventas.</li>
     *   <li>Mapeo de los resultados a {@link VentaListadoResponse}, incluyendo sus ítems.</li>
     * </ul>
     *
     * @param pageable datos de paginación (página, tamaño y ordenación)
     * @param tipoVenta tipo de venta expresado como cadena; se convierte a {@link TipoVenta}
     * @param nombreCliente filtro opcional por nombre del cliente
     * @param nombreUsuario filtro opcional por nombre del usuario que registró la venta
     * @param activa indica el estado de las ventas a recuperar:
     *               <ul>
     *                 <li>{@code true}: solo ventas activas</li>
     *                 <li>{@code false}: solo ventas anuladas</li>
     *                 <li>{@code null}: solo ventas activas</li>
     *               </ul>
     * @param desde fecha mínima del rango de consulta; puede ser {@code null}
     * @param hasta fecha máxima del rango de consulta; puede ser {@code null}
     * @return una instancia de {@link PaginaVentaResponse} con resultados agrupados
     */
    public PaginaVentaResponse traerTodos(Pageable pageable,
                                          String tipoVenta,
                                          String nombreCliente,
                                          String nombreUsuario,
                                          Boolean activa,
                                          LocalDateTime desde,
                                          LocalDateTime hasta) {

        log.debug("Listando ventas filtros tipoVenta={}, cliente={}, usuario={}, activa={}, desde={}, hasta={}",
                tipoVenta, nombreCliente, nombreUsuario, activa, desde, hasta);

        TipoVenta filtroTipoVenta = parseTipoVenta(tipoVenta);

        Specification<Venta> spec = VentaSpecification.conFiltros(
                filtroTipoVenta,
                nombreCliente,
                nombreUsuario,
                activa,
                desde,
                hasta
        );

        var pagina = ventaRepository.findAll(spec, pageable);
        List<VentaListadoResponse> contenido = pagina.stream()
                .map(ventaMapper::toListado)
                .toList();

        PaginaVentaResponse response = new PaginaVentaResponse();
        response.setContent(contenido);
        response.setPage(pagina.getNumber());
        response.setSize(pagina.getSize());
        response.setTotalPages(pagina.getTotalPages());
        response.setTotalElements(pagina.getTotalElements());
        return response;
    }


    /**
     * Crea una nueva venta y aplica los ajustes correspondientes en inventario,
     * capital y cartera según el tipo de venta (contado o crédito).
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que la solicitud contenga al menos un ítem.</li>
     *   <li>Obtener el usuario autenticado que registra la venta.</li>
     *   <li>Consultar el cliente asociado a la venta.</li>
     *   <li>Construir la entidad {@link Venta} a partir del request, resolviendo los productos
     *       mediante {@code productoService.buscarPorId}.</li>
     *   <li>Persistir la venta en la base de datos.</li>
     *   <li>Ajustar el inventario descontando las cantidades vendidas.</li>
     *   <li>Actualizar el módulo de capital:
     *       <ul>
     *         <li>Si es CONTADO → registrar el ingreso directo en capital.</li>
     *         <li>Si es CRÉDITO → registrar el ingreso diferido y enviar la venta a cartera.</li>
     *       </ul>
     *   </li>
     * </ul>
     *
     * @param request datos de la venta, incluyendo cliente, tipo de venta e ítems vendidos
     * @return un {@link VentaResponse} con la información completa de la venta creada
     * @throws IllegalArgumentException si la venta no contiene productos
     */
    @Transactional
    public VentaResponse crearVenta(VentaRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Debe enviar al menos un producto para crear la venta.");
        }

        Usuario usuario = usuarioService.obtenerUsuarioActual();

        Cliente cliente = clienteService.buscarPorId(request.getClienteId());

        Venta venta = ventaMapper.requestToEntity(request, usuario, cliente, productoService::buscarPorId);

        Venta guardada = ventaRepository.save(venta);
        log.info("Venta {} persistida, ajustando inventario", guardada.getId());
        ajustarInventarioPorVenta(guardada);
        if (guardada.getTipoVenta() == TipoVenta.CONTADO) {
            capitalService.registrarVentaContado(guardada);
        } else {
            capitalService.registrarVentaCredito(guardada);
            carteraService.registrarVentaEnCartera(guardada);
        }
        log.info("Venta {} creada con {} detalles", guardada.getId(), guardada.getDetalles().size());
        return ventaMapper.entityToResponse(guardada);
    }

    /**
     * Anula una venta existente, revierte su impacto en inventario, elimina o ajusta
     * el registro asociado en la cartera y corrige los movimientos financieros en el
     * módulo de capital según corresponda.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación del identificador y del motivo de la anulación.</li>
     *   <li>Recuperación de la venta y verificación de que aún se encuentre activa.</li>
     *   <li>Reversión del inventario mediante {@code revertirInventarioPorAnulacion}.</li>
     *   <li>Construcción del motivo formal de anulación, incluyendo fecha y usuario responsable.</li>
     *   <li>Actualización del estado de la venta como anulada.</li>
     *   <li>Reversión financiera a través de {@code capitalService.revertirVenta(venta)}.</li>
     *   <li>Ajustes correspondientes en cartera mediante {@code carteraService.ajustarPorAnulacion}.</li>
     *   <li>Persistencia de la venta con su estado actualizado.</li>
     * </ul>
     *
     * @param ventaId identificador de la venta a anular
     * @param razon motivo que justifica la anulación de la venta
     * @return un {@link VentaResponse} representando la venta ya anulada y actualizada
     * @throws ResponseStatusException si el ID es inválido, si no se proporciona un motivo,
     *                                 o si la venta ya estaba anulada
     * @throws EntityNotFoundException si no existe una venta con el ID especificado
     */
    @Transactional
    public VentaResponse anularVenta(Long ventaId, String razon) {
        if (ventaId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el id de la venta a anular.");
        }
        if (razon == null || razon.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el motivo de la anulación.");
        }

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + ventaId));

        if (!venta.isActiva()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La venta ya se encuentra anulada.");
        }

        log.info("Revirtiendo inventario para venta {}", ventaId);
        revertirInventarioPorAnulacion(venta);

        Usuario usuarioActual = usuarioService.obtenerUsuarioActual();
        String motivo = "La venta fue anulada el " + LocalDateTime.now() +
                " por el usuario " + usuarioActual.getUsuario() +
                " por el siguiente motivo: " + razon.trim();

        venta.setActiva(false);
        venta.setMotivoAnulacion(motivo.trim());
        capitalService.revertirVenta(venta);

        carteraService.ajustarPorAnulacion(venta, usuarioActual, motivo);

        Venta actualizada = ventaRepository.save(venta);
        log.info("Venta {} anulada. Motivo: {}", actualizada.getId(), actualizada.getMotivoAnulacion());
        return ventaMapper.entityToResponse(actualizada);
    }

    private TipoVenta parseTipoVenta(String tipoVenta) {
        if (tipoVenta == null || tipoVenta.isBlank()) {
            return null;
        }
        try {
            return TipoVenta.valueOf(tipoVenta.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tipo de venta inválido: " + tipoVenta, ex);
        }
    }

    private void ajustarInventarioPorVenta(Venta venta) {
        if (venta.getDetalles() == null) {
            return;
        }
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle == null) {
                continue;
            }
            Producto producto = detalle.getProducto();
            Integer cantidad = detalle.getCantidad();
            if (producto == null || producto.getId() == null || cantidad == null || cantidad <= 0) {
                continue;
            }
            String observacion = String.format("Venta #%d", venta.getId());
            log.debug("Registrando salida inventario por venta {} producto {} cantidad {}", venta.getId(), producto.getId(), cantidad);
            inventarioService.registrarSalida(producto.getId(), cantidad, observacion);
        }
    }

    private void revertirInventarioPorAnulacion(Venta venta) {
        if (venta.getDetalles() == null) {
            return;
        }
        for (DetalleVenta detalle : venta.getDetalles()) {
            if (detalle == null) {
                continue;
            }
            Producto producto = detalle.getProducto();
            Integer cantidad = detalle.getCantidad();
            if (producto == null || producto.getId() == null || cantidad == null || cantidad <= 0) {
                continue;
            }
            String observacion = String.format("Anulación venta #%d", venta.getId());
            log.debug("Registrando entrada inventario por anulación {} producto {} cantidad {}", venta.getId(), producto.getId(), cantidad);
            inventarioService.registrarDevolucion(producto.getId(), cantidad, observacion);
        }
    }
}
