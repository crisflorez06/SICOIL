package com.SICOIL.dtos.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioRequest {

    @NotBlank
    @Size(max = 100)
    private String usuario;

    @NotBlank
    @Size(min = 6, max = 120)
    private String contrasena;
}
