package com.SICOIL.services;

import com.SICOIL.dtos.producto.IngresoProductoRequest;
import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.ProductoIdPrecio;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.services.capital.CapitalService;
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
    private final CapitalService capitalService;

    /**
     * Registra la devolución de productos provenientes de una venta anulada,
     * restaurando la cantidad al inventario y actualizando la información financiera
     * correspondiente en el módulo de capital.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación de la cantidad devuelta (debe ser mayor a cero).</li>
     *   <li>Recuperación del producto y suma de la cantidad devuelta a su stock actual.</li>
     *   <li>Registro del movimiento de entrada en el kardex para mantener la trazabilidad
     *       del inventario.</li>
     *   <li>Registro del ingreso en el capital cuando el producto posee un precio de compra válido,
     *       reflejando así el retorno financiero equivalente a la devolución.</li>
     * </ul>
     *
     * @param productoId identificador del producto al que se le devolverá la cantidad
     * @param cantidad número de unidades devueltas tras la anulación de la venta
     * @param observacion comentario o motivo asociado a la devolución
     * @return la entidad {@link Producto} actualizada tras aplicar la devolución
     * @throws IllegalArgumentException si la cantidad es nula o menor o igual a cero
     * @throws EntityNotFoundException si no existe un producto con el identificador especificado
     */
    public Producto registrarDevolucion(Long productoId, Integer cantidad, String observacion) {
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

    /**
     * Registra la salida de stock para un producto, utilizada tanto al eliminar productos
     * como al realizar una venta u otros movimientos negativos. Este método delega el
     * registro en el kardex con tipo {@link MovimientoTipo#SALIDA}.
     *
     * @param productoId identificador del producto al cual se le realizará la salida
     * @param cantidad número de unidades a descontar del inventario
     * @param observacion comentario o motivo asociado a la salida
     * @return la entidad {@link Producto} actualizada después de la operación
     */
    public Producto registrarSalida(Long productoId, Integer cantidad, String observacion) {
        return registrarSalida(productoId, cantidad, observacion, MovimientoTipo.SALIDA);
    }

    /**
     * Registra la salida de stock para un producto permitiendo especificar el tipo de
     * movimiento que quedará trazado en el kardex (por ejemplo, {@link MovimientoTipo#VENTA}).
     * El método valida la cantidad solicitada, verifica el stock disponible e impacta
     * el kardex para mantener trazabilidad del movimiento.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación de la cantidad (debe ser mayor a cero).</li>
     *   <li>Recuperación del producto y cálculo del nuevo stock.</li>
     *   <li>Validación de stock insuficiente para evitar valores negativos.</li>
     *   <li>Actualización del inventario con la cantidad descontada.</li>
     *   <li>Registro del movimiento de salida en el kardex con el tipo solicitado.</li>
     * </ul>
     *
     * @param productoId identificador del producto al cual se le realizará la salida
     * @param cantidad número de unidades a descontar del inventario
     * @param observacion comentario o motivo asociado a la salida
     * @param tipoMovimiento tipo a registrar en el kardex; si es {@code null} se utilizará {@link MovimientoTipo#SALIDA}
     * @return la entidad {@link Producto} actualizada después de la operación
     * @throws IllegalArgumentException si la cantidad es inválida o si el stock disponible es insuficiente
     * @throws EntityNotFoundException si no existe un producto con el identificador especificado
     */
    public Producto registrarSalida(Long productoId,
                                    Integer cantidad,
                                    String observacion,
                                    MovimientoTipo tipoMovimiento) {
        if (cantidad == null || cantidad <= 0) {
            log.warn("Cantidad inválida ({}) al registrar salida para producto {}", cantidad, productoId);
            throw new IllegalArgumentException("La cantidad debe ser mayor a cero.");
        }

        MovimientoTipo tipoFinal = tipoMovimiento != null ? tipoMovimiento : MovimientoTipo.SALIDA;
        log.info("Registrando salida. Producto {} cantidad {} tipo {}", productoId, cantidad, tipoFinal);
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
        kardexService.registrarMovimiento(guardado, cantidad, observacion, tipoFinal);

        return guardado;
    }

    /**
     * Registra el ingreso de un producto al inventario, aplicando lógica para manejar variantes
     * basadas en el precio de compra.
     *
     * <p>El proceso funciona de la siguiente manera:
     * <ul>
     *   <li>Se consulta si existe una variante del mismo producto (mismo nombre) cuyo precio de compra
     *       coincida exactamente con el precio proporcionado en la solicitud.</li>
     *   <li>Si existe una variante con el mismo precio:
     *     <ul>
     *       <li>Se incrementa su stock con la cantidad ingresada.</li>
     *       <li>Se registra un movimiento de entrada en el kardex.</li>
     *       <li>Se registra el ajuste financiero en el módulo de capital según el precio de compra ingresado.</li>
     *     </ul>
     *   </li>
     *   <li>Si no existe una variante con ese precio:
     *     <ul>
     *       <li>Se busca un producto base por nombre.</li>
     *       <li>Se crea una nueva variante (nuevo producto) con el nuevo precio y el stock ingresado.</li>
     *       <li>Se registra el movimiento correspondiente en kardex.</li>
     *       <li>Se registra el ingreso financiero en capital.</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * @param request información del ingreso, incluyendo nombre del producto, precio de compra y cantidad
     * @return la entidad {@link Producto} creada o actualizada dependiendo de si existía una variante con el mismo precio
     * @throws EntityNotFoundException si no existe un producto base cuando se requiere crear una variante
     */
    @Transactional
    public Producto registrarIngresoProducto(IngresoProductoRequest request) {

        Producto productoDb = productoRepository.findFirstByNombreIgnoreCase(request.getNombreProducto())
                .orElseThrow(() -> new EntityNotFoundException("Producto base no encontrado"));

        Producto productoNuevoPrecio = Producto.builder()
                .nombre(productoDb.getNombre())
                .precioCompra(request.getPrecioCompra())
                .cantidadPorCajas(productoDb.getCantidadPorCajas())
                .stock(request.getCantidad())
                .comentario(request.getComentario())
                .build();

        Producto guardado = productoRepository.save(productoNuevoPrecio);
        kardexService.registrarMovimiento(guardado, request.getCantidad(), request.getComentario(), MovimientoTipo.ENTRADA);
        capitalService.registrarIngresoInventario(
                guardado,
                request.getPrecioCompra(),
                request.getCantidad(),
                "Ingreso de " + request.getCantidad() + " de " + productoDb.getNombre() + " con nuevo precio: " + (int)request.getPrecioCompra()
        );
        log.info("Creado producto {} por nuevo precio {} con stock {}", guardado.getId(), request.getPrecioCompra(), request.getCantidad());
        return guardado;
    }

    /**
     * Registra el stock inicial de un producto recién creado, generando tanto el
     * movimiento correspondiente en el kardex como el ajuste financiero asociado en
     * el módulo de capital, siempre y cuando el producto incluya una cantidad inicial válida.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación del stock inicial (debe ser mayor a cero para proceder).</li>
     *   <li>Registro del movimiento de entrada en el kardex con el stock inicial.</li>
     *   <li>Registro del ingreso financiero basado en el costo unitario del producto,
     *       solo si dicho costo es válido.</li>
     *   <li>Uso de la observación proporcionada o una descripción generada automáticamente.</li>
     * </ul>
     *
     * @param producto entidad del producto cuyo stock inicial será registrado
     * @param observacion texto descriptivo del movimiento; si es {@code null}, se genera uno por defecto
     */
    public void registrarStockInicial(Producto producto, String observacion) {
        Integer stockInicial = producto.getStock();
        if (stockInicial == null || stockInicial <= 0) {
            log.debug("Producto {} sin stock inicial para registrar en kardex", producto.getId());
            return;
        }
        log.info("Registrando movimiento de stock inicial para producto {} cantidad {}", producto.getId(), stockInicial);
        kardexService.registrarMovimiento(producto, stockInicial, observacion, MovimientoTipo.ENTRADA);
        Double costoUnitario = producto.getPrecioCompra();
        if (costoUnitario != null && costoUnitario > 0) {
            capitalService.registrarIngresoInventario(
                    producto,
                    costoUnitario,
                    stockInicial,
                    observacion != null ? observacion : "Stock inicial producto " + producto.getNombre() + " Cantidad: " + producto.getStock()
            );
        }
    }


}
