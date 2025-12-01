package com.SICOIL.dtos.capital;

import com.SICOIL.models.CapitalOrigen;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapitalMovimientoFiltro {

    private CapitalOrigen origen;
    private Boolean esCredito;
    private Long referenciaId;
    private String descripcion;
    private LocalDate desde;
    private LocalDate hasta;
}
