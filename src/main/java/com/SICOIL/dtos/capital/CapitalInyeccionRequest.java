package com.SICOIL.dtos.capital;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CapitalInyeccionRequest {

    @NotNull
    @Positive
    private Double monto;

    private String descripcion;

    private LocalDateTime fechaRegistro;

}
