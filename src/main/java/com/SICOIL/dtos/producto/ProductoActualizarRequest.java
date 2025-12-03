package com.SICOIL.dtos.producto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoActualizarRequest {
    @NotBlank
    private String nombre;

    @NotNull
    @PositiveOrZero
    private Integer cantidadPorCajas;

}
