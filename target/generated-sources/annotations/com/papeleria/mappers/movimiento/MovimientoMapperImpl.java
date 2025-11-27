package com.papeleria.mappers.movimiento;

import com.papeleria.dtos.MovimientoResponse;
import com.papeleria.models.Movimiento;
import com.papeleria.models.Producto;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-26T22:23:44-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (JetBrains s.r.o.)"
)
@Component
public class MovimientoMapperImpl implements MovimientoMapper {

    @Override
    public MovimientoResponse toResponse(Movimiento movimiento) {
        if ( movimiento == null ) {
            return null;
        }

        MovimientoResponse.MovimientoResponseBuilder movimientoResponse = MovimientoResponse.builder();

        movimientoResponse.productoId( movimientoProductoId( movimiento ) );
        movimientoResponse.productoNombre( movimientoProductoNombre( movimiento ) );
        movimientoResponse.id( movimiento.getId() );
        movimientoResponse.cantidad( movimiento.getCantidad() );
        movimientoResponse.tipo( movimiento.getTipo() );
        movimientoResponse.fechaMovimiento( movimiento.getFechaMovimiento() );
        movimientoResponse.observacion( movimiento.getObservacion() );

        return movimientoResponse.build();
    }

    private Long movimientoProductoId(Movimiento movimiento) {
        if ( movimiento == null ) {
            return null;
        }
        Producto producto = movimiento.getProducto();
        if ( producto == null ) {
            return null;
        }
        Long id = producto.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String movimientoProductoNombre(Movimiento movimiento) {
        if ( movimiento == null ) {
            return null;
        }
        Producto producto = movimiento.getProducto();
        if ( producto == null ) {
            return null;
        }
        String nombre = producto.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }
}
