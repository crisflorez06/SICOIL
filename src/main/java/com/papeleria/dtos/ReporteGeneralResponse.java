package com.papeleria.dtos;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReporteGeneralResponse {
    private BigDecimal totalGanancias;
    private BigDecimal totalGastos;
    private BigDecimal totalDineroEnVentas;
    private Long totalVentas;
    private List<ProductoMasVendidoResponse> productosMasVendidos;
}
