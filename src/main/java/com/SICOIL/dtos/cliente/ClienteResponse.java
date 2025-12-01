package com.SICOIL.dtos.cliente;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ClienteResponse {

    private Long id;
    private String nombre;
    private String telefono;
    private String direccion;
    private LocalDateTime fechaRegistro;
}
