package com.SICOIL.dtos.invetario;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class salidaRequest {
    @NotNull
    @Positive
    private Integer cantidad;

    private String observacion;
}
