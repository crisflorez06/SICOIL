package com.SICOIL.dtos;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MovimientoEntradaMasivaRequest {

    @NotEmpty
    @Valid
    private List<MovimientoEntradaProductoRequest> movimientos;
}
