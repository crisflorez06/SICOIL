package com.papeleria.controllers.producto;

import com.papeleria.dtos.MovimientoEntradaMasivaRequest;
import com.papeleria.dtos.MovimientoEntradaRequest;
import com.papeleria.dtos.ProductoRequest;
import com.papeleria.dtos.ProductoResponse;
import com.papeleria.mappers.producto.ProductoMapper;
import com.papeleria.models.Producto;
import com.papeleria.services.producto.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    @Autowired
    private ProductoMapper productoMapper;

    @GetMapping
    public ResponseEntity<?> obtenerTodosLosProductos(
            @PageableDefault(size = 10, sort = "nombre", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) Boolean estado) {
        try {
            Page<Producto> productos = productoService.buscarTodos(
                    pageable, nombre, categoria, estado);

            if (productos.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body("No hay productos disponibles");
            }

            Page<ProductoResponse> response = productos.map(productoMapper::toResponse);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error al obtener productos: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crearProducto(@Valid @RequestBody ProductoRequest productoRequest) {
        Producto productoGuardado = productoService.crearProducto(productoRequest);
        ProductoResponse response = productoMapper.toResponse(productoGuardado);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductoResponse> actualizarProducto(@PathVariable Long id, @Valid @RequestBody ProductoRequest productoRequest) {
        Producto productoActualizado = productoService.actualizarProducto(id, productoRequest);
        ProductoResponse response = productoMapper.toResponse(productoActualizado);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<ProductoResponse> cambiarEstadoProducto(@PathVariable Long id) {
        Producto productoActualizado = productoService.cambiarEstadoProducto(id);
        ProductoResponse response = productoMapper.toResponse(productoActualizado);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/{id}/agregar")
    public ResponseEntity<ProductoResponse> agregarCantidad(@PathVariable Long id, @RequestBody MovimientoEntradaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Debe proporcionar los datos del movimiento.");
        }

        Producto productoActualizado = productoService.agregarCantidad(id, request.getCantidad(), request.getObservacion());
        ProductoResponse response = productoMapper.toResponse(productoActualizado);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PatchMapping("/agregar-masivo")
    public ResponseEntity<?> agregarCantidadMasivo(@RequestBody MovimientoEntradaMasivaRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Debe proporcionar los datos de los movimientos.");
        }
        var movimientos = request.getMovimientos();
        if (movimientos == null || movimientos.isEmpty()) {
            return ResponseEntity.badRequest().body("Debe proporcionar al menos un producto a actualizar.");
        }

        var productosActualizados = productoService.agregarCantidadMasivo(movimientos);
        return ResponseEntity.ok(productoMapper.toResponseList(productosActualizados));
    }
}
