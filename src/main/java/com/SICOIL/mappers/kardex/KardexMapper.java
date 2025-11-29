package com.SICOIL.mappers.kardex;

import com.SICOIL.dtos.kardex.KardexResponse;
import com.SICOIL.models.Kardex;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface KardexMapper {

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombre")
    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "usuarioNombre", source = "usuario.usuario")
    KardexResponse entityToResponse(Kardex kardex);
}
