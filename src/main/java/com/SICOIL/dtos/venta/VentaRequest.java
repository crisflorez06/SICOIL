package com.SICOIL.dtos.venta;

import com.SICOIL.models.TipoVenta;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VentaRequest {

    @NotNull
    private Long clienteId;

    @NotNull
    private TipoVenta tipoVenta;


    @Valid
    @NotEmpty
    private List<DetalleVentaRequest> items;

}
