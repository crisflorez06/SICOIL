package com.SICOIL.dtos.cartera;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CarteraCreditoDetalleResponse {

    private Long movimientoId;
    private Long ventaId;
    private Double monto;
    private LocalDateTime fecha;
    private String usuarioNombre;
    private String observacion;
}
