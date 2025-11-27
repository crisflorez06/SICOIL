package com.papeleria.controllers.gasto;

import com.papeleria.models.Gasto;
import com.papeleria.services.gasto.GastoService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @GetMapping
    public ResponseEntity<List<Gasto>> listarGastos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<Gasto> gastos = gastoService.listar(desde, hasta, nombre);
        if (gastos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(gastos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Gasto> obtenerGasto(@PathVariable Long id) {
        Gasto gasto = gastoService.obtenerPorId(id);
        return ResponseEntity.ok(gasto);
    }

    @PostMapping
    public ResponseEntity<Gasto> crearGasto(@Valid @RequestBody Gasto gasto) {
        Gasto gastoCreado = gastoService.crear(gasto);
        return new ResponseEntity<>(gastoCreado, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Gasto> actualizarGasto(@PathVariable Long id, @Valid @RequestBody Gasto gasto) {
        Gasto gastoActualizado = gastoService.actualizar(id, gasto);
        return ResponseEntity.ok(gastoActualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGasto(@PathVariable Long id) {
        gastoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
