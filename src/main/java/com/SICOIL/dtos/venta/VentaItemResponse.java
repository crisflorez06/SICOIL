package com.SICOIL.dtos.venta;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VentaItemResponse {

    private String productoNombre;
    private Double precioCompra;
    private Integer cantidad;
    private Double precioVenta;
}
