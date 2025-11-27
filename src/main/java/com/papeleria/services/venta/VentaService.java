package com.papeleria.services.venta;
import static com.papeleria.services.venta.DetalleVentaSpecification.productoNombreContains;
import static com.papeleria.services.venta.DetalleVentaSpecification.ventaFechaBetween;
import static com.papeleria.services.venta.VentaSpecification.*;

import com.papeleria.dtos.*;
import com.papeleria.mappers.venta.DetalleVentaMapper;
import com.papeleria.models.DetalleVenta;
import com.papeleria.models.Producto;
import com.papeleria.models.Venta;
import com.papeleria.repositories.DetalleVentaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import com.papeleria.mappers.venta.VentaMapper;
import com.papeleria.repositories.ProductoRepository;
import com.papeleria.repositories.VentaRepository;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private VentaMapper ventaMapper;
    @Autowired
    private DetalleVentaMapper detalleVentaMapper;




    public VentaPageResponse traerTodos(Pageable pageable,
                                        String metodoPago,
                                        LocalDateTime desde,
                                        LocalDateTime hasta,
                                        BigDecimal minTotal,
                                        BigDecimal maxTotal) {

        if (desde == null && hasta == null) {
            desde = LocalDate.now().atStartOfDay();
            hasta = LocalDate.now().atTime(23, 59, 59);
        }

        Specification<Venta> spec = Specification
                .where(metodoPagoEquals(metodoPago))
                .and(fechaBetween(desde, hasta))
                .and(totalMin(minTotal))
                .and(totalMax(maxTotal))
                .and(totalMayorQueCero());

        Page<VentaResponse> pageDeVentas = ventaRepository.findAll(spec, pageable).map(ventaMapper::toResponse);

        List<Venta> todasLasVentas = ventaRepository.findAll(spec);

        BigDecimal totalGeneral = todasLasVentas.stream()
                .map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new VentaPageResponse(pageDeVentas, totalGeneral);
    }

    public Page<DetalleVentaResponse> buscarDetalles(Pageable pageable,
                                                     String nombreProducto,
                                                     LocalDateTime desde,
                                                     LocalDateTime hasta) {

        // Default to today if no date range is provided by the user for filtering
        if (desde == null && hasta == null) {
            desde = LocalDate.now().atStartOfDay();
            hasta = LocalDate.now().atTime(23, 59, 59);
        }

        Specification<DetalleVenta> spec = Specification.where(productoNombreContains(nombreProducto))
                .and(ventaFechaBetween(desde, hasta));

        return detalleVentaRepository.findAll(spec, pageable).map(detalleVentaMapper::toResponse);
    }

    public List<DetalleVentaResponse> obtenerDetallesPorVenta(Long ventaId) {
        return ventaRepository.findById(ventaId)
                .map(Venta::getDetalles)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada"))
                .stream()
                .map(detalleVentaMapper::toResponse)
                .toList();
    }

    @Transactional
    public Venta crearVenta(VentaRequest request) {
        Venta venta = new Venta();
        venta.setFecha(LocalDateTime.now());
        venta.setMetodoPago(request.getMetodoPago());

        BigDecimal total = BigDecimal.ZERO;
        List<Producto> productosParaActualizar = new ArrayList<>();

        for (DetalleVentaRequest detalleReq : request.getDetalles()) {
            if (detalleReq.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad del producto debe ser mayor a cero.");
            }

            Producto producto = productoRepository.findById(detalleReq.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + detalleReq.getProductoId()));

            if (producto.getStock() < detalleReq.getCantidad()) {
                throw new IllegalArgumentException("No hay stock suficiente para el producto: " + producto.getNombre());
            }

            BigDecimal subtotal = producto.getPrecioVenta().multiply(BigDecimal.valueOf(detalleReq.getCantidad()));

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(detalleReq.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecioVenta());
            detalle.setSubtotal(subtotal);

            // Establecer la relación bidireccional correctamente
            venta.agregarDetalle(detalle);
            total = total.add(subtotal);

            producto.setStock(producto.getStock() - detalleReq.getCantidad());
            productosParaActualizar.add(producto);
        }

        productoRepository.saveAll(productosParaActualizar);

        venta.setTotal(total);

        return ventaRepository.save(venta);
    }

    @Transactional
    public Venta actualizarVenta(Long ventaId, VentaRequest request) {
        Venta ventaExistente = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + ventaId));

        List<Producto> productosParaActualizar = new ArrayList<>();
        for (DetalleVenta detalleAntiguo : ventaExistente.getDetalles()) {
            Producto producto = detalleAntiguo.getProducto();
            producto.setStock(producto.getStock() + detalleAntiguo.getCantidad());
            productosParaActualizar.add(producto);
        }

        ventaExistente.getDetalles().clear();
        BigDecimal nuevoTotal = BigDecimal.ZERO;

        for (DetalleVentaRequest detalleNuevoReq : request.getDetalles()) {
            Producto producto = productoRepository.findById(detalleNuevoReq.getProductoId())
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con ID: " + detalleNuevoReq.getProductoId()));

            Producto productoEnLista = productosParaActualizar.stream()
                .filter(p -> p.getId().equals(producto.getId())).findFirst().orElse(producto);

            if (productoEnLista.getStock() < detalleNuevoReq.getCantidad()) {
                throw new IllegalArgumentException("No hay stock suficiente para el producto: " + productoEnLista.getNombre());
            }

            productoEnLista.setStock(productoEnLista.getStock() - detalleNuevoReq.getCantidad());
            if (!productosParaActualizar.contains(productoEnLista)) {
                productosParaActualizar.add(productoEnLista);
            }

            DetalleVenta detalleNuevo = new DetalleVenta();
            detalleNuevo.setProducto(productoEnLista);
            detalleNuevo.setCantidad(detalleNuevoReq.getCantidad());
            detalleNuevo.setPrecioUnitario(productoEnLista.getPrecioVenta());
            BigDecimal subtotal = productoEnLista.getPrecioVenta().multiply(BigDecimal.valueOf(detalleNuevoReq.getCantidad()));
            detalleNuevo.setSubtotal(subtotal);

            ventaExistente.agregarDetalle(detalleNuevo);
            nuevoTotal = nuevoTotal.add(subtotal);
        }

        productoRepository.saveAll(productosParaActualizar);

        ventaExistente.setTotal(nuevoTotal);
        ventaExistente.setMetodoPago(request.getMetodoPago());

        return ventaRepository.save(ventaExistente);
    }

    @Transactional
    public void eliminarVenta(Long ventaId) {
        Venta ventaExistente = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + ventaId));

        List<Producto> productosParaActualizar = new ArrayList<>();
        Set<Long> productosProcesados = new HashSet<>();

        for (DetalleVenta detalle : ventaExistente.getDetalles()) {
            Producto producto = detalle.getProducto();
            producto.setStock(producto.getStock() + detalle.getCantidad());

            if (producto.getId() != null && productosProcesados.add(producto.getId())) {
                productosParaActualizar.add(producto);
            } else if (producto.getId() == null) {
                productosParaActualizar.add(producto);
            }
        }

        if (!productosParaActualizar.isEmpty()) {
            productoRepository.saveAll(productosParaActualizar);
        }

        ventaExistente.getDetalles().clear();
        ventaExistente.setTotal(BigDecimal.ZERO);

        ventaRepository.save(ventaExistente);
    }


}
