package com.SICOIL.dtos.invetario;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class entradaPrecioNuevoRequest {

    @NotNull
    @Positive
    private Integer cantidad;

    @NotNull
    @Positive
    private Double precioNuevo;

    private String observacion;
}
