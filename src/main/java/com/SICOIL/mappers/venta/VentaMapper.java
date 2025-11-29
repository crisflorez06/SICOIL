package com.SICOIL.mappers.venta;

import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.models.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring", uses = {DetalleVentaMapper.class})
public interface VentaMapper {

//    @Mapping(target = "detalles", source = "detalles")
//    @Mapping(target = "usuarioId", source = "usuario.id")
//    @Mapping(target = "usuarioNombre", source = "usuario.nombre")
//    VentaResponse toResponse(Venta venta);
//
//    List<VentaResponse> toResponseList(List<Venta> ventas);
}
