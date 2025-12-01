package com.SICOIL.dtos.cliente;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteRequest {

    @NotBlank
    @Size(max = 150)
    private String nombre;

    @Size(max = 20)
    private String telefono;

    @Size(max = 200)
    private String direccion;
}
