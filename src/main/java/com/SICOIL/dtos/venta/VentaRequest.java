package com.SICOIL.dtos.venta;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VentaRequest {

    @NotBlank
    private String metodoPago;

    @NotNull
    private Long usuarioId;

    @Valid
    @NotEmpty
    private List<DetalleVentaRequest> detalles;
}
