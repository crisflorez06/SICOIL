package com.SICOIL.dtos.reporte;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TotalResponse {

    private String periodo;
    private BigDecimal total;
}
