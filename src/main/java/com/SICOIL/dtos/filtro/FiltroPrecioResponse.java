package com.SICOIL.dtos.filtro;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FiltroPrecioResponse {
    private Long id;
    private Double precioCompra;
    private Integer cantidad;
}
