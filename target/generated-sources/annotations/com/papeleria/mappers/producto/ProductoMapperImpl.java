package com.papeleria.mappers.producto;

import com.papeleria.dtos.ProductoRequest;
import com.papeleria.dtos.ProductoResponse;
import com.papeleria.models.Producto;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-11-26T22:23:44-0500",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 21.0.8 (JetBrains s.r.o.)"
)
@Component
public class ProductoMapperImpl implements ProductoMapper {

    @Override
    public Producto toEntity(ProductoRequest request) {
        if ( request == null ) {
            return null;
        }

        Producto.ProductoBuilder producto = Producto.builder();

        producto.nombre( request.getNombre() );
        producto.descripcion( request.getDescripcion() );
        producto.precioCompra( request.getPrecioCompra() );
        producto.precioVenta( request.getPrecioVenta() );
        producto.stock( request.getStock() );
        producto.categoria( request.getCategoria() );

        return producto.build();
    }

    @Override
    public ProductoResponse toResponse(Producto producto) {
        if ( producto == null ) {
            return null;
        }

        ProductoResponse.ProductoResponseBuilder productoResponse = ProductoResponse.builder();

        productoResponse.id( producto.getId() );
        productoResponse.nombre( producto.getNombre() );
        productoResponse.descripcion( producto.getDescripcion() );
        productoResponse.precioCompra( producto.getPrecioCompra() );
        productoResponse.precioVenta( producto.getPrecioVenta() );
        productoResponse.stock( producto.getStock() );
        productoResponse.categoria( producto.getCategoria() );
        productoResponse.fechaRegistro( producto.getFechaRegistro() );
        productoResponse.estado( producto.getEstado() );

        return productoResponse.build();
    }

    @Override
    public List<ProductoResponse> toResponseList(List<Producto> productos) {
        if ( productos == null ) {
            return null;
        }

        List<ProductoResponse> list = new ArrayList<ProductoResponse>( productos.size() );
        for ( Producto producto : productos ) {
            list.add( toResponse( producto ) );
        }

        return list;
    }

    @Override
    public void updateEntityFromRequest(ProductoRequest request, Producto producto) {
        if ( request == null ) {
            return;
        }

        if ( request.getNombre() != null ) {
            producto.setNombre( request.getNombre() );
        }
        if ( request.getDescripcion() != null ) {
            producto.setDescripcion( request.getDescripcion() );
        }
        if ( request.getPrecioCompra() != null ) {
            producto.setPrecioCompra( request.getPrecioCompra() );
        }
        if ( request.getPrecioVenta() != null ) {
            producto.setPrecioVenta( request.getPrecioVenta() );
        }
        if ( request.getStock() != null ) {
            producto.setStock( request.getStock() );
        }
        if ( request.getCategoria() != null ) {
            producto.setCategoria( request.getCategoria() );
        }
    }
}
