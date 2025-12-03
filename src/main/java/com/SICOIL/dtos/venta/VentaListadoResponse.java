package com.SICOIL.dtos.venta;

import com.SICOIL.models.TipoVenta;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VentaListadoResponse {

    private Long ventaId;
    private String clienteNombre;
    private Double totalVenta;
    private TipoVenta tipoVenta;
    private boolean activa;
    private String motivoAnulacion;
    private String usuarioNombre;
    private LocalDateTime fechaRegistro;
    private List<VentaItemResponse> items;
}
