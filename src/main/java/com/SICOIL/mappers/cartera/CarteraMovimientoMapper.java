package com.SICOIL.mappers.cartera;

import com.SICOIL.dtos.cartera.CarteraAbonoDetalleResponse;
import com.SICOIL.dtos.cartera.CarteraCreditoDetalleResponse;
import com.SICOIL.models.CarteraMovimiento;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CarteraMovimientoMapper {

    @Mapping(target = "movimientoId", source = "id")
    @Mapping(target = "usuarioNombre", source = "usuario.usuario")
    CarteraAbonoDetalleResponse toAbonoResponse(CarteraMovimiento movimiento);

    @Mapping(target = "movimientoId", source = "id")
    @Mapping(target = "usuarioNombre", source = "usuario.usuario")
    @Mapping(target = "ventaId", source = "cartera.venta.id")
    CarteraCreditoDetalleResponse toCreditoResponse(CarteraMovimiento movimiento);
}
