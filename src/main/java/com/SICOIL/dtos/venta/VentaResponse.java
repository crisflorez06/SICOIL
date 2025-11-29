package com.SICOIL.dtos.venta;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VentaResponse {

    private Long id;
    private LocalDateTime fecha;
    private BigDecimal total;
    private String metodoPago;
    private Long usuarioId;
    private String usuarioNombre;
    private List<DetalleVentaResponse> detalles;
}
