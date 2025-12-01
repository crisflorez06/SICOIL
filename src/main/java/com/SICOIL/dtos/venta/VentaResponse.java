package com.SICOIL.dtos.venta;

import java.time.LocalDateTime;
import java.util.List;

import com.SICOIL.models.TipoVenta;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaResponse {

    private Long id;
    private Long clienteId;
    private String clienteNombre;
    private Long usuarioId;
    private String usuarioNombre;
    private TipoVenta tipoVenta;
    private boolean activa;
    private String motivoAnulacion;
    private Double total;
    private LocalDateTime fechaRegistro;
    private List<DetalleVentaResponse> detalles;
}
