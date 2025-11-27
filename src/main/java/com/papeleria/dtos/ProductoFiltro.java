package com.papeleria.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoFiltro {
    private Long id;
    private String nombre;
    private BigDecimal precioVenta;
}
