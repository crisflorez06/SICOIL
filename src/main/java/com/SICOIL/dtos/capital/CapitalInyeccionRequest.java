package com.SICOIL.dtos.capital;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CapitalInyeccionRequest {

    @NotNull
    @Positive
    private Double monto;

    private String descripcion;
}
