package com.papeleria.mappers.venta;

import com.papeleria.dtos.DetalleVentaResponse;
import com.papeleria.dtos.VentaResponse;
import com.papeleria.models.DetalleVenta;
import com.papeleria.models.Venta;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-26T22:23:44-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (JetBrains s.r.o.)"
)
@Component
public class VentaMapperImpl implements VentaMapper {

    @Autowired
    private DetalleVentaMapper detalleVentaMapper;

    @Override
    public VentaResponse toResponse(Venta venta) {
        if ( venta == null ) {
            return null;
        }

        VentaResponse.VentaResponseBuilder ventaResponse = VentaResponse.builder();

        ventaResponse.detalles( detalleVentaListToDetalleVentaResponseList( venta.getDetalles() ) );
        ventaResponse.id( venta.getId() );
        ventaResponse.fecha( venta.getFecha() );
        ventaResponse.total( venta.getTotal() );
        ventaResponse.metodoPago( venta.getMetodoPago() );

        return ventaResponse.build();
    }

    @Override
    public List<VentaResponse> toResponseList(List<Venta> ventas) {
        if ( ventas == null ) {
            return null;
        }

        List<VentaResponse> list = new ArrayList<VentaResponse>( ventas.size() );
        for ( Venta venta : ventas ) {
            list.add( toResponse( venta ) );
        }

        return list;
    }

    protected List<DetalleVentaResponse> detalleVentaListToDetalleVentaResponseList(List<DetalleVenta> list) {
        if ( list == null ) {
            return null;
        }

        List<DetalleVentaResponse> list1 = new ArrayList<DetalleVentaResponse>( list.size() );
        for ( DetalleVenta detalleVenta : list ) {
            list1.add( detalleVentaMapper.toResponse( detalleVenta ) );
        }

        return list1;
    }
}
