package com.SICOIL.dtos.producto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ProductosAgrupadosResponse {
    private String nombre;          // nombre del producto (agrupado)
    private Integer stockTotal;     // suma de stock de todas las variantes
    private Integer cantidadPorCajas; // la presentación
    private List<ProductosDesagrupadosResponse> variantes;
}
