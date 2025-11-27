package com.SICOIL.mappers.movimiento;

import com.SICOIL.dtos.MovimientoResponse;
import com.SICOIL.models.Movimiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    MovimientoResponse toResponse(Movimiento movimiento);
}
