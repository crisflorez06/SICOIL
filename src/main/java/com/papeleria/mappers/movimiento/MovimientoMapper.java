package com.papeleria.mappers.movimiento;

import com.papeleria.dtos.MovimientoResponse;
import com.papeleria.models.Movimiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface MovimientoMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    MovimientoResponse toResponse(Movimiento movimiento);
}
