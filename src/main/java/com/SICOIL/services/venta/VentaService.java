package com.SICOIL.services.venta;

import static com.SICOIL.services.venta.DetalleVentaSpecification.clienteNombreContains;
import static com.SICOIL.services.venta.DetalleVentaSpecification.productoNombreContains;
import static com.SICOIL.services.venta.DetalleVentaSpecification.tipoVentaEquals;
import static com.SICOIL.services.venta.DetalleVentaSpecification.ventaActivaEquals;
import static com.SICOIL.services.venta.DetalleVentaSpecification.usuarioNombreContains;
import static com.SICOIL.services.venta.DetalleVentaSpecification.ventaFechaBetween;

import com.SICOIL.dtos.venta.*;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.DetalleVenta;
import com.SICOIL.models.Producto;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.DetalleVentaRepository;
import com.SICOIL.mappers.venta.VentaMapper;
import com.SICOIL.repositories.VentaRepository;
import com.SICOIL.services.InventarioService;
import com.SICOIL.services.producto.ProductoService;
import com.SICOIL.services.usuario.UsuarioService;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    private final DetalleVentaRepository detalleVentaRepository;
    private final UsuarioService usuarioService;
    private final ClienteService clienteService;
    private final VentaMapper ventaMapper;
    private final InventarioService inventarioService;

    public Page<VentaDetalleTablaResponse> traerTodos(Pageable pageable,
                                                      String nombreProducto,
                                                      String tipoVenta,
                                                      String nombreCliente,
                                                      String nombreUsuario,
                                                      Boolean activa,
                                                      LocalDateTime desde,
                                                      LocalDateTime hasta) {

        log.debug("Listando ventas filtros producto={}, tipoVenta={}, cliente={}, usuario={}, activa={}, desde={}, hasta={}",
                nombreProducto, tipoVenta, nombreCliente, nombreUsuario, activa, desde, hasta);

        TipoVenta filtroTipoVenta = parseTipoVenta(tipoVenta);

        Specification<DetalleVenta> spec = Specification
                .where(productoNombreContains(nombreProducto))
                .and(tipoVentaEquals(filtroTipoVenta))
                .and(ventaActivaEquals(activa))
                .and(clienteNombreContains(nombreCliente))
                .and(usuarioNombreContains(nombreUsuario))
                .and(ventaFechaBetween(desde, hasta));

        return detalleVentaRepository.findAll(spec, pageable).map(ventaMapper::toResponse);
    }


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
        log.info("Venta {} creada con {} detalles", guardada.getId(), guardada.getDetalles().size());
        return ventaMapper.entityToResponse(guardada);
    }

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

        String usuarioActual = usuarioService.obtenerUsuarioActual().getUsuario();
        String motivo = "La venta fue anulada el " + LocalDateTime.now() +
                " por el usuario " + usuarioActual +
                " por el siguiente motivo: " + razon.trim();

        venta.setActiva(false);
        venta.setMotivoAnulacion(motivo.trim());

        Venta actualizada = ventaRepository.save(venta);
        log.info("Venta {} anulada. Motivo: {}", actualizada.getId(), actualizada.getMotivoAnulacion());
        return ventaMapper.entityToResponse(actualizada);
    }



//    @Transactional
//    public Venta actualizarVenta(Long ventaId, VentaRequest request) {
//        Venta ventaExistente = ventaRepository.findById(ventaId)
//                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + ventaId));
//
//        Usuario usuario = usuarioRepository.findById(request.getUsuarioId())
//                .orElseThrow(() -> new EntityNotFoundException("Usuario no encontrado con ID: " + request.getUsuarioId()));
//
//        List<Producto> productosParaActualizar = new ArrayList<>();
//        Set<Long> procesados = new HashSet<>();
//
//        for (DetalleVenta detalleAntiguo : ventaExistente.getDetalles()) {
//            Producto producto = detalleAntiguo.getProducto();
//            if (producto == null) {
//                continue;
//            }
//            incrementarStock(producto, detalleAntiguo.getCantidad());
//            registrarProductoParaActualizar(producto, procesados, productosParaActualizar);
//        }
//
//        ventaExistente.getDetalles().clear();
//        BigDecimal nuevoTotal = BigDecimal.ZERO;
//
//        for (DetalleVentaRequest detalleNuevoReq : request.getDetalles()) {
//            validarCantidad(detalleNuevoReq.getCantidad());
//            Producto producto = obtenerProducto(detalleNuevoReq.getProductoId());
//            descontarStock(producto, detalleNuevoReq.getCantidad());
//            registrarProductoParaActualizar(producto, procesados, productosParaActualizar);
//
//            BigDecimal subtotal = calcularSubtotal(producto, detalleNuevoReq.getCantidad());
//            DetalleVenta detalleNuevo = crearDetalleVenta(producto, detalleNuevoReq.getCantidad(), subtotal);
//
//            ventaExistente.agregarDetalle(detalleNuevo);
//            nuevoTotal = nuevoTotal.add(subtotal);
//        }
//
//        if (!productosParaActualizar.isEmpty()) {
//            productoRepository.saveAll(productosParaActualizar);
//        }
//
//        ventaExistente.setTotal(nuevoTotal.doubleValue());
//        ventaExistente.setEstado(obtenerEstadoDesdeMetodoPago(request.getMetodoPago()));
//        ventaExistente.setUsuario(usuario);
//
//        Venta actualizada = ventaRepository.save(ventaExistente);
//        log.info("Venta {} actualizada", actualizada.getId());
//        return actualizada;
//    }
//
//    @Transactional
//    public void eliminarVenta(Long ventaId) {
//        Venta ventaExistente = ventaRepository.findById(ventaId)
//                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + ventaId));
//
//        List<Producto> productosParaActualizar = new ArrayList<>();
//        Set<Long> productosProcesados = new HashSet<>();
//
//        for (DetalleVenta detalle : ventaExistente.getDetalles()) {
//            Producto producto = detalle.getProducto();
//            if (producto == null) {
//                continue;
//            }
//            incrementarStock(producto, detalle.getCantidad());
//            registrarProductoParaActualizar(producto, productosProcesados, productosParaActualizar);
//        }
//
//        if (!productosParaActualizar.isEmpty()) {
//            productoRepository.saveAll(productosParaActualizar);
//        }
//
//        ventaExistente.getDetalles().clear();
//        ventaExistente.setTotal(0d);
//        ventaExistente.setEstado("ANULADA");
//        ventaRepository.save(ventaExistente);
//        log.info("Venta {} eliminada", ventaId);
//    }
//
//    private void registrarProductoParaActualizar(Producto producto,
//                                                 Set<Long> procesados,
//                                                 List<Producto> productosParaActualizar) {
//        Long productoId = producto.getId();
//        if (productoId == null || procesados.add(productoId)) {
//            productosParaActualizar.add(producto);
//        }
//    }
//
//    private Producto obtenerProducto(Long productoId) {
//        return productoRepository.findById(productoId)
//                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + productoId));
//    }
//
//    private BigDecimal calcularSubtotal(Producto producto, int cantidad) {
//        return obtenerPrecioVenta(producto).multiply(BigDecimal.valueOf(cantidad));
//    }
//
//    private DetalleVenta crearDetalleVenta(Producto producto, int cantidad, BigDecimal subtotal) {
//        DetalleVenta detalle = new DetalleVenta();
//        detalle.setProducto(producto);
//        detalle.setCantidad(cantidad);
//        detalle.setSubtotal(subtotal.doubleValue());
//        detalle.setTipoVenta(TipoVenta.CONTADO);
//        return detalle;
//    }
//
//    private void validarCantidad(int cantidad) {
//        if (cantidad <= 0) {
//            throw new IllegalArgumentException("La cantidad del producto debe ser mayor a cero.");
//        }
//    }
//
//    private void descontarStock(Producto producto, int cantidad) {
//        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
//        int nuevoStock = stockActual - cantidad;
//        if (nuevoStock < 0) {
//            throw new IllegalArgumentException("No hay stock suficiente para el producto: " + producto.getNombre());
//        }
//        producto.setStock(nuevoStock);
//    }
//
//    private void incrementarStock(Producto producto, int cantidad) {
//        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
//        producto.setStock(stockActual + cantidad);
//    }
//
//    private String obtenerEstadoDesdeMetodoPago(String metodoPago) {
//        return (metodoPago == null || metodoPago.isBlank()) ? ESTADO_POR_DEFECTO : metodoPago;
//    }
//
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
            inventarioService.registrarMovimiento(producto.getId(), cantidad, observacion);
        }
    }
//
//    private BigDecimal obtenerPrecioVenta(Producto producto) {
//        Double precioCompra = producto.getPrecioCompra();
//        if (precioCompra == null) {
//            log.warn("Producto {} no tiene precio configurado, se usará 0", producto.getId());
//            return BigDecimal.ZERO;
//        }
//        return BigDecimal.valueOf(precioCompra);
//    }
//
//    private DetalleVentaResponse mapDetalleToResponse(DetalleVenta detalle) {
//        Producto producto = detalle.getProducto();
//        return DetalleVentaResponse.builder()
//                .id(detalle.getId())
//                .productoId(producto != null ? producto.getId() : null)
//                .productoNombre(producto != null ? producto.getNombre() : null)
//                .cantidad(detalle.getCantidad())
//                .precioUnitario(calcularPrecioUnitario(detalle))
//                .subtotal(convertirADecimal(detalle.getSubtotal()))
//                .fecha(detalle.getVenta() != null ? detalle.getVenta().getFechaRegistro() : null)
//                .build();
//    }
//
//    private BigDecimal calcularPrecioUnitario(DetalleVenta detalle) {
//        Integer cantidad = detalle.getCantidad();
//        Double subtotal = detalle.getSubtotal();
//        if (cantidad == null || cantidad == 0 || subtotal == null) {
//            return BigDecimal.ZERO;
//        }
//        return BigDecimal.valueOf(subtotal)
//                .divide(BigDecimal.valueOf(cantidad), 2, RoundingMode.HALF_UP);
//    }
//
//    private BigDecimal convertirADecimal(Double valor) {
//        return valor == null ? BigDecimal.ZERO : BigDecimal.valueOf(valor);
//    }
}
