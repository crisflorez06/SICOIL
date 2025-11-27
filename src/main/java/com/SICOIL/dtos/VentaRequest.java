package com.SICOIL.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VentaRequest {

    @NotBlank
    private String metodoPago;

    @Valid
    @NotEmpty
    private List<DetalleVentaRequest> detalles;
}
