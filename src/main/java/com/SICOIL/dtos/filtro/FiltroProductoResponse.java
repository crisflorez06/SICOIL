package com.SICOIL.dtos.filtro;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FiltroProductoResponse {
    private String nombreProducto;
    private Integer cantidadPorCajas;
    private List<FiltroPrecioResponse> precios;
}
