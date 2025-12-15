package com.SICOIL.dtos.capital;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapitalVentaMensual {

    private String mes;
    private double total;
}
