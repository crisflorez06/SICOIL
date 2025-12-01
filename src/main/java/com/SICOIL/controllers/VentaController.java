package com.SICOIL.controllers;

import com.SICOIL.dtos.venta.VentaAnulacionRequest;
import com.SICOIL.dtos.venta.VentaDetalleTablaResponse;
import com.SICOIL.dtos.venta.VentaRequest;
import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.services.venta.VentaService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    public ResponseEntity<Page<VentaDetalleTablaResponse>> traerTodos(
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false) String nombreProducto,
            @RequestParam(required = false) String tipoVenta,
            @RequestParam(required = false) String nombreCliente,
            @RequestParam(required = false) String nombreUsuario,
            @RequestParam(required = false) Boolean activa,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {

        LocalDateTime desdeDateTime = desde != null ? desde.atStartOfDay() : null;
        LocalDateTime hastaDateTime = hasta != null ? hasta.atTime(23, 59, 59) : null;

        Page<VentaDetalleTablaResponse> pagina = ventaService.traerTodos(
                pageable,
                nombreProducto,
                tipoVenta,
                nombreCliente,
                nombreUsuario,
                activa,
                desdeDateTime,
                hastaDateTime
        );

        return ResponseEntity.ok(pagina);
    }

    @PostMapping
    public ResponseEntity<VentaResponse> crearVenta(@Valid @RequestBody VentaRequest request) {
        VentaResponse response = ventaService.crearVenta(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PatchMapping("/{ventaId}/anular")
    public ResponseEntity<VentaResponse> anularVenta(@PathVariable Long ventaId,
                                                     @Valid @RequestBody VentaAnulacionRequest request) {
        VentaResponse response = ventaService.anularVenta(ventaId, request.getMotivo());
        return ResponseEntity.ok(response);
    }
}
