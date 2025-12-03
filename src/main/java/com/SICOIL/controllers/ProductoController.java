package com.SICOIL.controllers;

import com.SICOIL.dtos.invetario.entradaPrecioExistenteRequest;
import com.SICOIL.dtos.invetario.entradaPrecioNuevoRequest;
import com.SICOIL.dtos.invetario.salidaRequest;
import com.SICOIL.dtos.producto.IngresoProductoRequest;
import com.SICOIL.dtos.producto.PaginaProductoResponse;
import com.SICOIL.dtos.producto.ProductoActualizarRequest;
import com.SICOIL.dtos.producto.ProductoRequest;
import com.SICOIL.dtos.producto.ProductoResponse;
import com.SICOIL.services.producto.ProductoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    public ResponseEntity<?> traerTodos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String nombre
    ){
        return ResponseEntity.ok(
                productoService.traerTodos(nombre, page, size)
        );
    }


    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest productoRequest) {
        ProductoResponse response = productoService.crearProducto(productoRequest);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{nombre}")
    public ResponseEntity<Boolean> actualizarProducto(
            @PathVariable String nombre,
            @Valid @RequestBody ProductoActualizarRequest productoRequest) {

        boolean actualizado = productoService.actualizarProducto(nombre, productoRequest);
        return ResponseEntity.ok(actualizado);
    }


    @PostMapping("/ingreso")
    public List<ProductoResponse> registrarIngresos(@RequestBody List<IngresoProductoRequest> lista) {
        return productoService.registrarIngresoProductos(lista);
    }



    @PatchMapping("/{id}/stock/eliminar")
    public ResponseEntity<ProductoResponse> eliminarCantidad(
            @PathVariable Long id,
            @Valid @RequestBody salidaRequest request) {

        ProductoResponse response =
                productoService.eliminarCantidad(id, request.getCantidad(), request.getObservacion());

        return ResponseEntity.ok(response);
    }
}
