package com.papeleria.dtos;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductoMasVendidoResponse {

    private Long productoId;
    private String nombre;
    private Long cantidadVendida;
    private BigDecimal totalGenerado;
}
