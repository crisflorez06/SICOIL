package com.SICOIL.dtos.cartera;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarteraAbonoRequest {

    @NotNull
    @Positive
    private Double monto;

    @Size(max = 500)
    private String observacion;
}
