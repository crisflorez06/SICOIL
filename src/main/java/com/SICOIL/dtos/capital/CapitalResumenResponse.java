package com.SICOIL.dtos.capital;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CapitalResumenResponse {

    private double saldoReal;
    private double totalEntradas;
    private double totalSalidas;
    private double totalCreditoPendiente;
    private double totalCredito;
    private double capitalNeto;
}
