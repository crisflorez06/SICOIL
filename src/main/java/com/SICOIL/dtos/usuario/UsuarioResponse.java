package com.SICOIL.dtos.usuario;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UsuarioResponse {

    private final Long id;
    private final String usuario;
}
