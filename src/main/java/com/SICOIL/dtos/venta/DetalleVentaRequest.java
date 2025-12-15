package com.SICOIL.dtos.venta;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DetalleVentaRequest {
    @NotNull
    private String nombreProducto;

    @NotNull
    @Positive
    private Integer cantidad;

    @NotNull
    @PositiveOrZero
    private Double subtotal;
}
