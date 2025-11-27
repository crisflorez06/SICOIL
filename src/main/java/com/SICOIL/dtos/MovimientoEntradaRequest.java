package com.SICOIL.dtos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimientoEntradaRequest {

    @NotNull
    @Positive
    private Integer cantidad;

    private String observacion;
}
