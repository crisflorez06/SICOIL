package com.SICOIL.dtos.capital;

import java.util.List;
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
    private double totalGanancias;
    private double totalAbonos;
    private double totalUnidadesVendidas;
    private double totalCajasVendidas;
    private List<CapitalProductoResumen> topProductos;
    private List<CapitalClienteResumen> topClientes;
}
