package com.SICOIL.mappers.producto;

import com.SICOIL.dtos.producto.ProductoRequest;
import com.SICOIL.dtos.producto.ProductoResponse;
import com.SICOIL.models.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaRegistro", source = "fechaRegistro")
    Producto requestToEntity(ProductoRequest request);

    @Mapping(target = "fechaRegistro", source = "fechaRegistro")
    @Mapping(target = "id", ignore = true)
    void updateEntityFromRequest(ProductoRequest request, @MappingTarget Producto entity);

    ProductoResponse entitytoResponse(Producto producto);
}
