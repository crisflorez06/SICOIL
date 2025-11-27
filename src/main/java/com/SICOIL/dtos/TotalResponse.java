package com.SICOIL.dtos;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TotalResponse {

    private String periodo;
    private BigDecimal total;
}
