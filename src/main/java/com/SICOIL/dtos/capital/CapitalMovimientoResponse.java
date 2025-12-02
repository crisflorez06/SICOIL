package com.SICOIL.dtos.capital;

import com.SICOIL.models.CapitalOrigen;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapitalMovimientoResponse {

    private Long id;
    private CapitalOrigen origen;
    private Long referenciaId;
    private Double monto;
    private Boolean esCredito;
    private String descripcion;
    private LocalDateTime creadoEn;
    private Long usuarioId;
    private String usuarioNombre;
}
