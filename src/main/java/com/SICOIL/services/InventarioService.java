package com.SICOIL.services;

import com.SICOIL.dtos.producto.IngresoProductoRequest;
import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.ProductoIdPrecio;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.services.kardex.KardexService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final KardexService kardexService;

    public Producto registrarMovimiento(Long productoId, Integer cantidad, String observacion) {
        if (cantidad == null || cantidad <= 0) {
            log.warn("Cantidad inválida ({}) al registrar entrada existente para producto {}", cantidad, productoId);
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        log.info("Registrando entrada existente. Producto {} cantidad {}", productoId, cantidad);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + productoId));

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
        producto.setStock(stockActual + cantidad);

        Producto guardado = productoRepository.save(producto);
        kardexService.registrarMovimiento(guardado, cantidad, observacion, MovimientoTipo.ENTRADA);

        return guardado;
    }

    public Producto registrarSalida(Long productoId, Integer cantidad, String observacion) {
        if (cantidad == null || cantidad <= 0) {
            log.warn("Cantidad inválida ({}) al registrar salida para producto {}", cantidad, productoId);
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        log.info("Registrando salida. Producto {} cantidad {}", productoId, cantidad);
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + productoId));

        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
        int nuevoStock = stockActual - cantidad;

        if (nuevoStock < 0) {
            log.warn("Stock insuficiente para producto {}. Actual: {}, solicitado: {}", productoId, stockActual, cantidad);
            throw new IllegalArgumentException(
                    "El stock disponible (" + stockActual + ") es insuficiente para descontar la cantidad solicitada (" + cantidad + ").");
        }

        producto.setStock(nuevoStock);

        Producto guardado = productoRepository.save(producto);
        kardexService.registrarMovimiento(guardado, cantidad, observacion, MovimientoTipo.SALIDA);

        return guardado;
    }

    public Producto registrarIngresoProducto(IngresoProductoRequest request) {

        List<ProductoIdPrecio> listaPrecios = productoRepository.findIdAndPrecioByNombre(request.getNombreProducto());


        Long idProducto = listaPrecios.stream()
                .filter(p -> Double.compare(p.getPrecioCompra(), request.getPrecioCompra()) == 0)
                .map(ProductoIdPrecio::getId)
                .findFirst()
                .orElse(null);

        if(idProducto != null) {
            log.info("Registrando ingreso de producto {} cantidad {} precio {}", request.getNombreProducto(), request.getCantidad(), request.getPrecioCompra());

            Producto productoDb = productoRepository.findById(idProducto)
                    .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + idProducto));

            int stockActual = productoDb.getStock() != null ? productoDb.getStock() : 0;
                productoDb.setStock(stockActual + request.getCantidad());
                Producto guardado = productoRepository.save(productoDb);
                kardexService.registrarMovimiento(guardado, request.getCantidad(), null, MovimientoTipo.ENTRADA);
                log.info("Actualizado stock de producto {} manteniendo precio. Nuevo stock {}", guardado.getId(), guardado.getStock());
                return guardado;

        }

        Producto productoDb = productoRepository.findFirstByNombreIgnoreCase(request.getNombreProducto())
                .orElseThrow(() -> new EntityNotFoundException("Producto base no encontrado"));


        // Si el precio es distinto, creas un nuevo producto (misma lógica que tenías)
        Producto productoNuevoPrecio = new Producto(
                productoDb.getNombre(),
                request.getPrecioCompra(),
                productoDb.getCantidadPorCajas(),
                request.getCantidad()
        );

        Producto guardado = productoRepository.save(productoNuevoPrecio);
        kardexService.registrarMovimiento(guardado, request.getCantidad(), null, MovimientoTipo.ENTRADA);
        log.info("Creado producto {} por nuevo precio {} con stock {}", guardado.getId(), request.getPrecioCompra(), request.getCantidad());
        return guardado;
    }

    public void registrarStockInicial(Producto producto, String observacion) {
        Integer stockInicial = producto.getStock();
        if (stockInicial == null || stockInicial <= 0) {
            log.debug("Producto {} sin stock inicial para registrar en kardex", producto.getId());
            return;
        }
        log.info("Registrando movimiento de stock inicial para producto {} cantidad {}", producto.getId(), stockInicial);
        kardexService.registrarMovimiento(producto, stockInicial, observacion, MovimientoTipo.ENTRADA);
    }
}
