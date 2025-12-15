package com.SICOIL.dtos.producto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductoResponse {

    private Long id;
    private String nombre;
    private Double precioCompra;
    private Integer cantidadPorCajas;
    private Integer stock;
    private String comentario;
}
