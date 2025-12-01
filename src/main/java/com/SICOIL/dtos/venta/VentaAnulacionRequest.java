package com.SICOIL.dtos.venta;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VentaAnulacionRequest {

    @NotBlank(message = "Debe indicar el motivo de la anulación.")
    private String motivo;
}
