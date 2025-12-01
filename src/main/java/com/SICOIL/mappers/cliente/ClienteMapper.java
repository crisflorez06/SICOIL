package com.SICOIL.mappers.cliente;

import com.SICOIL.dtos.cliente.ClienteRequest;
import com.SICOIL.dtos.cliente.ClienteResponse;
import com.SICOIL.models.Cliente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClienteMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fechaRegistro", ignore = true)
    Cliente requestToEntity(ClienteRequest request);

    ClienteResponse entityToResponse(Cliente cliente);
}
