package com.SICOIL.dtos.cartera;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CarteraResumenResponse {

    private Long clienteId;
    private String clienteNombre;
    private Double saldoPendiente;
    private Double totalAbonos;
    private Double totalCreditos;
    private LocalDateTime ultimaActualizacion;
}
