package com.papeleria.services;

import java.util.List;
import java.util.stream.Collectors;

import com.papeleria.dtos.FiltrosDTO;
import com.papeleria.dtos.ProductoFiltro;
import com.papeleria.models.Producto;
import com.papeleria.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class FiltroService {

    @Autowired
    private ProductoRepository productoRepository;

    public FiltrosDTO getFiltros() {
        // 1️⃣ Traemos todos los productos
        List<Producto> productos = productoRepository.findAll();

        // 2️⃣ Creamos una lista con solo id y nombre
        List<ProductoFiltro> nombresProductos = productos.stream()
                .map(p -> new ProductoFiltro(p.getId(), p.getNombre(), p.getPrecioVenta()))
                .collect(Collectors.toList());

        // 3️⃣ Traemos las categorías distintas
        List<String> categoriasProductos = productoRepository.findDistinctCategoria();

        // 4️⃣ Retornamos el DTO completo
        return new FiltrosDTO(nombresProductos, categoriasProductos);
    }
}