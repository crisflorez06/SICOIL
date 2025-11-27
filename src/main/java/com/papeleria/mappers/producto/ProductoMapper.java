package com.papeleria.mappers.producto;

import com.papeleria.dtos.ProductoRequest;
import com.papeleria.dtos.ProductoResponse;
import com.papeleria.models.Producto;
import java.util.List;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @BeanMapping(ignoreByDefault = true)
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "precioCompra", source = "precioCompra")
    @Mapping(target = "precioVenta", source = "precioVenta")
    @Mapping(target = "stock", source = "stock")
    @Mapping(target = "categoria", source = "categoria")
    Producto toEntity(ProductoRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "nombre", source = "nombre")
    @Mapping(target = "descripcion", source = "descripcion")
    @Mapping(target = "precioCompra", source = "precioCompra")
    @Mapping(target = "precioVenta", source = "precioVenta")
    @Mapping(target = "stock", source = "stock")
    @Mapping(target = "categoria", source = "categoria")
    @Mapping(target = "fechaRegistro", source = "fechaRegistro")
    @Mapping(target = "estado", source = "estado")
    ProductoResponse toResponse(Producto producto);

    List<ProductoResponse> toResponseList(List<Producto> productos);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ProductoRequest request, @MappingTarget Producto producto);
}
