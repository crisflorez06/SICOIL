package com.SICOIL.services.producto;

import com.SICOIL.dtos.MovimientoEntradaProductoRequest;
import com.SICOIL.dtos.ProductoRequest;
import com.SICOIL.mappers.producto.ProductoMapper;
import com.SICOIL.models.Producto;
import com.SICOIL.services.movimiento.MovimientoService;
import com.SICOIL.repositories.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductoService {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ProductoMapper productoMapper;

    @Autowired
    private MovimientoService movimientoService;

    public Page<Producto> buscarTodos(Pageable pageable, String nombre, String categoria, Boolean estado) {

        Specification<Producto> spec = Specification.where(ProductoSpecification.hasNombre(nombre))
                .and(ProductoSpecification.hasCategoria(categoria));

        if (estado != null && !estado) {
            spec = spec.and(ProductoSpecification.hasEstado(false));
        } else {
            spec = spec.and(ProductoSpecification.hasEstado(true));
        }

        return productoRepository.findAll(spec, pageable);
    }

    public Producto crearProducto(ProductoRequest productoRequest) {
        String nombreNormalizado = normalizeNombre(productoRequest.getNombre());
        if (productoRepository.existsByNombreIgnoreCase(nombreNormalizado)) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + nombreNormalizado);
        }

        productoRequest.setNombre(nombreNormalizado);
        Producto producto = productoMapper.toEntity(productoRequest);
        Producto productoGuardado = productoRepository.save(producto);

        if (productoGuardado.getStock() != null && productoGuardado.getStock() > 0) {
            movimientoService.registrarIngreso(productoGuardado, productoGuardado.getStock(), "Stock inicial del producto");
        }

        return productoGuardado;
    }

    public Producto actualizarProducto(Long id, ProductoRequest productoRequest) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        String nombreNormalizado = normalizeNombre(productoRequest.getNombre());
        productoRepository.findByNombreIgnoreCase(nombreNormalizado)
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un producto con el nombre: " + nombreNormalizado);
                });

        productoRequest.setNombre(nombreNormalizado);

        productoMapper.updateEntityFromRequest(productoRequest, producto);
        return productoRepository.save(producto);
    }

    public Producto cambiarEstadoProducto(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        producto.setEstado(!producto.getEstado());
        return productoRepository.save(producto);
    }

    public Producto agregarCantidad(Long id, Integer cantidad, String observacion) {
        if (cantidad == null || cantidad == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero.");
        }

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
        int nuevoStock = stockActual + cantidad;
        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "El stock disponible (" + stockActual + ") es insuficiente para descontar la cantidad solicitada (" + Math.abs(cantidad) + ").");
        }

        producto.setStock(nuevoStock);
        Producto productoActualizado = productoRepository.save(producto);
        movimientoService.registrarIngreso(productoActualizado, cantidad, observacion);
        return productoActualizado;
    }

    private String normalizeNombre(String nombre) {
        return nombre == null ? null : nombre.trim();
    }

    public List<Producto> agregarCantidadMasivo(List<MovimientoEntradaProductoRequest> movimientos) {
        if (movimientos == null || movimientos.isEmpty()) {
            throw new IllegalArgumentException("Debe proporcionar al menos un producto para actualizar.");
        }

        Map<Long, Producto> productosPorId = productoRepository.findAllById(
                        movimientos.stream()
                                .map(movimiento -> {
                                    if (movimiento.getProductoId() == null) {
                                        throw new IllegalArgumentException("El identificador del producto es obligatorio.");
                                    }
                                    return movimiento.getProductoId();
                                })
                                .collect(Collectors.toSet()))
                .stream()
                .collect(Collectors.toMap(Producto::getId, producto -> producto));

        movimientos.forEach(movimiento -> {
            Long productoId = movimiento.getProductoId();
            Producto producto = productosPorId.get(productoId);
            if (producto == null) {
                throw new EntityNotFoundException("Producto no encontrado con id: " + productoId);
            }

            Integer cantidad = movimiento.getCantidad();
            if (cantidad == null || cantidad == 0) {
                throw new IllegalArgumentException("La cantidad no puede ser cero para el producto con id: " + productoId);
            }

            int stockActual = producto.getStock() != null ? producto.getStock() : 0;
            int nuevoStock = stockActual + cantidad;
            if (nuevoStock < 0) {
                throw new IllegalArgumentException(
                        "El stock disponible para el producto con id " + productoId + " es insuficiente (stock actual: "
                                + stockActual + ", cantidad solicitada: " + Math.abs(cantidad) + ").");
            }

            producto.setStock(nuevoStock);
            movimientoService.registrarIngreso(producto, cantidad, movimiento.getObservacion());
        });

        return productoRepository.saveAll(productosPorId.values());
    }
}
