package com.SICOIL.dtos.producto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PaginaProductoResponse {

    private List<ProductosAgrupadosResponse> content;

    private int page;
    private int size;
    private int totalPages;
    private long totalElements;
}
