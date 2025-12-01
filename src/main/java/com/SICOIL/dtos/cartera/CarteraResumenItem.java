package com.SICOIL.dtos.cartera;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarteraResumenItem {

    private Long clienteId;
    private String clienteNombre;
    private Double saldoPendiente;
    private Double totalAbonos;
    private Double totalCreditos;
    private LocalDateTime ultimaActualizacion;
}
