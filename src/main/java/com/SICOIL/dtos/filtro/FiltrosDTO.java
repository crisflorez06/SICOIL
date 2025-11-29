package com.SICOIL.dtos.filtro;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiltrosDTO {
    private List<ProductoFiltro> nombresProductos;
    private List<String> categoriasProductos;
}