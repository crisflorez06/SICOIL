//package com.SICOIL.controllers;
//
//import com.SICOIL.models.Compra;
//import com.SICOIL.services.gasto.GastoService;
//import jakarta.validation.Valid;
//import java.time.LocalDate;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.DeleteMapping;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.PutMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/gastos")
//public class GastoController {
//
//    @Autowired
//    private GastoService gastoService;
//
//    @GetMapping
//    public ResponseEntity<List<Compra>> listarGastos(
//            @RequestParam(required = false) String nombre,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
//            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
//        List<Compra> compras = gastoService.listar(desde, hasta, nombre);
//        if (compras.isEmpty()) {
//            return ResponseEntity.noContent().build();
//        }
//        return ResponseEntity.ok(compras);
//    }
//
//    @GetMapping("/{id}")
//    public ResponseEntity<Compra> obtenerGasto(@PathVariable Long id) {
//        Compra compra = gastoService.obtenerPorId(id);
//        return ResponseEntity.ok(compra);
//    }
//
//    @PostMapping
//    public ResponseEntity<Compra> crearGasto(@Valid @RequestBody Compra compra) {
//        Compra compraCreado = gastoService.crear(compra);
//        return new ResponseEntity<>(compraCreado, HttpStatus.CREATED);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<Compra> actualizarGasto(@PathVariable Long id, @Valid @RequestBody Compra compra) {
//        Compra compraActualizado = gastoService.actualizar(id, compra);
//        return ResponseEntity.ok(compraActualizado);
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> eliminarGasto(@PathVariable Long id) {
//        gastoService.eliminar(id);
//        return ResponseEntity.noContent().build();
//    }
//}
