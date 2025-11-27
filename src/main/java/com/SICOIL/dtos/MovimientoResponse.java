package com.SICOIL.dtos;

import com.SICOIL.models.MovimientoTipo;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MovimientoResponse {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Integer cantidad;
    private MovimientoTipo tipo;
    private LocalDateTime fechaMovimiento;
    private String observacion;
}
