package com.SICOIL.mappers.cartera;

import com.SICOIL.dtos.cartera.CarteraResumenItem;
import com.SICOIL.dtos.cartera.CarteraResumenResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CarteraMapper {

    CarteraResumenResponse toResumenResponse(CarteraResumenItem item);
}
