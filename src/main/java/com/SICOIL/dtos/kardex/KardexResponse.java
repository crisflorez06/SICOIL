package com.SICOIL.dtos.kardex;

import com.SICOIL.models.MovimientoTipo;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KardexResponse {

    private Long id;
    private Long productoId;
    private String productoNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private Integer cantidad;
    private MovimientoTipo tipo;
    private LocalDateTime fechaRegistro;
    private String comentario;
}
