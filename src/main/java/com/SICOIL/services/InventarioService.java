package com.SICOIL.services;

import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.services.kardex.KardexService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final KardexService kardexService;

    public Producto registrarEntradaExistente(Long productoId, Integer cantidad, String observacion) {
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

    public Producto registrarEntradaNuevoPrecio(Long productoId,
                                                Integer cantidad,
                                                Double precioNuevo,
                                                String observacion) {
        if (cantidad == null || cantidad <= 0) {
            log.warn("Cantidad inválida ({}) al registrar entrada nuevo precio para producto {}", cantidad, productoId);
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }
        if (precioNuevo == null || precioNuevo < 0) {
            log.warn("Precio inválido ({}) al registrar entrada nuevo precio para producto {}", precioNuevo, productoId);
            throw new IllegalArgumentException("El precio nuevo debe ser mayor o igual a cero.");
        }

        log.info("Registrando entrada con nuevo precio. Producto {} cantidad {} precio {}", productoId, cantidad, precioNuevo);
        Producto productoDb = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + productoId));

        // Si el precio es el mismo, solo sumas stock
        if (precioNuevo.equals(productoDb.getPrecioCompra())) {
            int stockActual = productoDb.getStock() != null ? productoDb.getStock() : 0;
            productoDb.setStock(stockActual + cantidad);
            Producto guardado = productoRepository.save(productoDb);
            kardexService.registrarMovimiento(guardado, cantidad, observacion, MovimientoTipo.ENTRADA);
            return guardado;
        }

        // Si el precio es distinto, creas un nuevo producto (misma lógica que tenías)
        Producto productoNuevoPrecio = new Producto(
                productoDb.getNombre(),
                precioNuevo,
                productoDb.getCantidadPorCajas(),
                cantidad
        );

        Producto guardado = productoRepository.save(productoNuevoPrecio);
        kardexService.registrarMovimiento(guardado, cantidad, observacion, MovimientoTipo.ENTRADA);
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
