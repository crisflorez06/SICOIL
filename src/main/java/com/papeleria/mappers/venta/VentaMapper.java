package com.papeleria.mappers.venta;

import com.papeleria.dtos.VentaResponse;
import com.papeleria.models.Venta;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring", uses = {DetalleVentaMapper.class})
public interface VentaMapper {

    @Mapping(target = "detalles", source = "detalles")
    VentaResponse toResponse(Venta venta);

    List<VentaResponse> toResponseList(List<Venta> ventas);
}
