package com.SICOIL.dtos.producto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductoRequest {

    @NotBlank
    private String nombre;

    @NotNull
    @PositiveOrZero
    private Integer cantidadPorCajas;

    @NotNull
    @PositiveOrZero
    private double precioCompra;

    @NotNull
    @PositiveOrZero
    private Integer stock;

}
