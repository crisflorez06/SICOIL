package com.SICOIL.dtos.filtro;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FiltroClienteResponse {
    private Long id;
    private String nombre;
}

