package com.papeleria.services.movimiento;

import static com.papeleria.services.movimiento.MovimientoSpecification.cantidadDistintaDeCero;
import static com.papeleria.services.movimiento.MovimientoSpecification.fechaBetween;
import static com.papeleria.services.movimiento.MovimientoSpecification.productoIdEquals;
import static com.papeleria.services.movimiento.MovimientoSpecification.tipoEquals;

import com.papeleria.models.Movimiento;
import com.papeleria.models.MovimientoTipo;
import com.papeleria.models.Producto;
import com.papeleria.repositories.MovimientoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MovimientoService {

    @Autowired
    private MovimientoRepository movimientoRepository;

    public Movimiento registrarIngreso(Producto producto, Integer cantidad, String observacion) {
        Movimiento movimiento = Movimiento.builder()
                .producto(producto)
                .cantidad(cantidad)
                .tipo(MovimientoTipo.INGRESO)
                .observacion(observacion)
                .build();
        return movimientoRepository.save(movimiento);
    }

    @Transactional(readOnly = true)
    public Page<Movimiento> buscar(Pageable pageable, Long productoId, MovimientoTipo tipo, LocalDate desde, LocalDate hasta) {
        boolean sinFiltros = productoId == null && tipo == null && desde == null && hasta == null;
        if (sinFiltros) {
            LocalDate hoy = LocalDate.now();
            desde = hoy;
            hasta = hoy;
        }

        Specification<Movimiento> spec = Specification.where(cantidadDistintaDeCero())
                .and(productoIdEquals(productoId))
                .and(tipoEquals(tipo))
                .and(fechaBetween(desde, hasta));

        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "fechaMovimiento");
        Pageable pageableConOrden = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return movimientoRepository.findAll(spec, pageableConOrden);
    }

    public Movimiento actualizarMovimiento(Long id, Integer cantidad, String observacion) {
        if (cantidad == null || cantidad == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser cero.");
        }

        Movimiento movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento no encontrado con id: " + id));

        Integer cantidadActual = movimiento.getCantidad();
        if (cantidadActual == null) {
            cantidadActual = 0;
        }

        if (cantidad.equals(cantidadActual)) {
            movimiento.setObservacion(observacion);
            return movimiento;
        }

        Producto producto = movimiento.getProducto();
        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
        int diferencia = cantidad - cantidadActual;
        int nuevoStock = stockActual + diferencia;

        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "El stock disponible (" + stockActual + ") es insuficiente para ajustar la cantidad del movimiento.");
        }

        producto.setStock(nuevoStock);
        movimiento.setCantidad(cantidad);
        movimiento.setObservacion(observacion);

        return movimiento;
    }

    public Movimiento eliminarMovimiento(Long id, String observacion) {
        Movimiento movimiento = movimientoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Movimiento no encontrado con id: " + id));

        Integer cantidadActual = movimiento.getCantidad();
        if (cantidadActual == null || cantidadActual == 0) {
            movimiento.setObservacion(observacion);
            return movimiento;
        }

        Producto producto = movimiento.getProducto();
        int stockActual = producto.getStock() != null ? producto.getStock() : 0;
        int nuevoStock = stockActual - cantidadActual;

        if (nuevoStock < 0) {
            throw new IllegalArgumentException(
                    "El stock disponible (" + stockActual + ") es insuficiente para revertir el movimiento.");
        }

        producto.setStock(nuevoStock);
        movimiento.setCantidad(0);
        movimiento.setObservacion(observacion);

        return movimiento;
    }
}
