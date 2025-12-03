package com.SICOIL.dtos.filtro;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FiltrosResponse {
    private List<FiltroProductoResponse> productos;
    private List<FiltroClienteResponse> clientes;
}

