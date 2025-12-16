package com.SICOIL.dtos.producto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class IngresoProductoRequest {

    @NotNull
    private String nombreProducto;

    @NotNull
    @PositiveOrZero
    private double precioCompra;

    @NotNull
    @PositiveOrZero
    private Integer cantidad;

    private String comentario;

    private LocalDateTime fechaRegistro;


}
