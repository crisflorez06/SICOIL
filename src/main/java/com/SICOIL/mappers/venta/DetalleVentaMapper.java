package com.SICOIL.mappers.venta;

import com.SICOIL.dtos.DetalleVentaResponse;
import com.SICOIL.models.DetalleVenta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DetalleVentaMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "fecha", source = "venta.fecha")
    DetalleVentaResponse toResponse(DetalleVenta detalle);
}
