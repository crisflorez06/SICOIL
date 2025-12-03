package com.SICOIL.dtos.capital;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapitalClienteResumen {

    private Long clienteId;
    private String clienteNombre;
    private long totalVentas;
    private double montoComprado;
    private double participacionPorcentaje;
}
