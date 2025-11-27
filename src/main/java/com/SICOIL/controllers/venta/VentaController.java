package com.SICOIL.controllers.venta;

import com.SICOIL.dtos.DetalleVentaResponse;
import com.SICOIL.dtos.VentaPageResponse;
import com.SICOIL.dtos.VentaRequest;
import com.SICOIL.dtos.VentaResponse;
import com.SICOIL.mappers.venta.VentaMapper;
import com.SICOIL.models.Venta;
import com.SICOIL.services.venta.VentaService;
import jakarta.validation.Valid;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private VentaMapper ventaMapper;

    @GetMapping
    public ResponseEntity<VentaPageResponse> traerTodos(
            @PageableDefault(size = 10, sort = "fecha", direction = org.springframework.data.domain.Sort.Direction.ASC)
            Pageable pageable,
            @RequestParam(required = false) String metodoPago,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) BigDecimal minTotal,
            @RequestParam(required = false) BigDecimal maxTotal) {

        LocalDateTime desdeDateTime = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = (hasta != null) ? hasta.atTime(23, 59, 59) : null;

        VentaPageResponse response = ventaService.traerTodos(pageable, metodoPago, desdeDateTime, hastaDateTime, minTotal, maxTotal);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/detalles")
    public ResponseEntity<Page<DetalleVentaResponse>> buscarDetalles(
            @PageableDefault(size = 10, sort = "venta.fecha", direction = org.springframework.data.domain.Sort.Direction.ASC)
            Pageable pageable,
            @RequestParam(required = false) String nombreProducto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {

        LocalDateTime desdeDateTime = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = (hasta != null) ? hasta.atTime(23, 59, 59) : null;

        Page<DetalleVentaResponse> page = ventaService.buscarDetalles(pageable, nombreProducto, desdeDateTime, hastaDateTime);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{ventaId}/detalles")
    public ResponseEntity<List<DetalleVentaResponse>> obtenerDetallesPorVenta(@PathVariable Long ventaId) {
        List<DetalleVentaResponse> detalles = ventaService.obtenerDetallesPorVenta(ventaId);
        return ResponseEntity.ok(detalles);
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crearVenta(@Valid @RequestBody VentaRequest request) {
        Venta venta = ventaService.crearVenta(request);
        VentaResponse response = ventaMapper.toResponse(venta);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PutMapping("/{ventaId}")
    public ResponseEntity<VentaResponse> actualizarVenta(@PathVariable Long ventaId, @Valid @RequestBody VentaRequest request) {
        Venta ventaActualizada = ventaService.actualizarVenta(ventaId, request);
        VentaResponse response = ventaMapper.toResponse(ventaActualizada);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{ventaId}")
    public ResponseEntity<Void> eliminarVenta(@PathVariable Long ventaId) {
        ventaService.eliminarVenta(ventaId);
        return ResponseEntity.noContent().build();
    }


}
