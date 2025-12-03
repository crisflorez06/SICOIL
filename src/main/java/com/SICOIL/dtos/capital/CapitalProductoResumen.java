package com.SICOIL.dtos.capital;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapitalProductoResumen {

    private Long productoId;
    private String productoNombre;
    private long cantidadVendida;
    private double totalVendido;
    private double participacionPorcentaje;
}
