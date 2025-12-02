package com.SICOIL.mappers.capital;

import com.SICOIL.dtos.capital.CapitalMovimientoResponse;
import com.SICOIL.models.CapitalMovimiento;
import org.springframework.stereotype.Component;

@Component
public class CapitalMovimientoMapper {

    public CapitalMovimientoResponse toResponse(CapitalMovimiento movimiento) {
        if (movimiento == null) {
            return null;
        }
        return CapitalMovimientoResponse.builder()
                .id(movimiento.getId())
                .origen(movimiento.getOrigen())
                .referenciaId(movimiento.getReferenciaId())
                .monto(movimiento.getMonto())
                .esCredito(movimiento.getEsCredito())
                .descripcion(movimiento.getDescripcion())
                .creadoEn(movimiento.getCreadoEn())
                .usuarioId(movimiento.getUsuario() != null ? movimiento.getUsuario().getId() : null)
                .usuarioNombre(movimiento.getUsuario() != null ? movimiento.getUsuario().getUsuario() : null)
                .build();
    }
}
