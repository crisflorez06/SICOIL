package com.SICOIL.dtos.venta;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaginaVentaResponse {

    private List<VentaListadoResponse> content;
    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
}
