package com.papeleria.mappers.venta;

import com.papeleria.dtos.DetalleVentaResponse;
import com.papeleria.models.DetalleVenta;
import com.papeleria.models.Producto;
import com.papeleria.models.Venta;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-26T22:23:44-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (JetBrains s.r.o.)"
)
@Component
public class DetalleVentaMapperImpl implements DetalleVentaMapper {

    @Override
    public DetalleVentaResponse toResponse(DetalleVenta detalle) {
        if ( detalle == null ) {
            return null;
        }

        DetalleVentaResponse.DetalleVentaResponseBuilder detalleVentaResponse = DetalleVentaResponse.builder();

        detalleVentaResponse.productoId( detalleProductoId( detalle ) );
        detalleVentaResponse.productoNombre( detalleProductoNombre( detalle ) );
        detalleVentaResponse.fecha( detalleVentaFecha( detalle ) );
        detalleVentaResponse.id( detalle.getId() );
        detalleVentaResponse.cantidad( detalle.getCantidad() );
        detalleVentaResponse.precioUnitario( detalle.getPrecioUnitario() );
        detalleVentaResponse.subtotal( detalle.getSubtotal() );

        return detalleVentaResponse.build();
    }

    private Long detalleProductoId(DetalleVenta detalleVenta) {
        if ( detalleVenta == null ) {
            return null;
        }
        Producto producto = detalleVenta.getProducto();
        if ( producto == null ) {
            return null;
        }
        Long id = producto.getId();
        if ( id == null ) {
            return null;
        }
        return id;
    }

    private String detalleProductoNombre(DetalleVenta detalleVenta) {
        if ( detalleVenta == null ) {
            return null;
        }
        Producto producto = detalleVenta.getProducto();
        if ( producto == null ) {
            return null;
        }
        String nombre = producto.getNombre();
        if ( nombre == null ) {
            return null;
        }
        return nombre;
    }

    private LocalDateTime detalleVentaFecha(DetalleVenta detalleVenta) {
        if ( detalleVenta == null ) {
            return null;
        }
        Venta venta = detalleVenta.getVenta();
        if ( venta == null ) {
            return null;
        }
        LocalDateTime fecha = venta.getFecha();
        if ( fecha == null ) {
            return null;
        }
        return fecha;
    }
}
