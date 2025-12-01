package com.SICOIL.dtos.venta;

import java.time.LocalDateTime;

import com.SICOIL.models.TipoVenta;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VentaDetalleTablaResponse {

    private Long detalleId;
    private String productoNombre;
    private Double precioCompra;
    private Integer cantidad;
    private Double subtotal;
    private TipoVenta tipoVenta;
    private boolean activa;
    private String motivoAnulacion;
    private String clienteNombre;
    private String usuarioNombre;
    private LocalDateTime fechaRegistro;
}
