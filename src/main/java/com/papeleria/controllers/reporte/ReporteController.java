package com.papeleria.controllers.reporte;

import com.papeleria.dtos.ReporteGeneralResponse;
import com.papeleria.dtos.ProductoResponse;
import com.papeleria.services.reporte.ReporteService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping("/generales")
    public ResponseEntity<ReporteGeneralResponse> getReporteGeneral(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        ReporteGeneralResponse reporte = reporteService.getReporteGeneral(fechaInicio, fechaFin);
        return ResponseEntity.ok(reporte);
    }

    @GetMapping("/stock-bajo")
    public ResponseEntity<List<ProductoResponse>> getProductosConStockBajo(
        @RequestParam Optional<Integer> umbral) {
        List<ProductoResponse> productos = reporteService.getProductosConStockBajo(umbral);
        return ResponseEntity.ok(productos);
    }
}
