package com.SICOIL.controllers;

import com.SICOIL.dtos.capital.CapitalInyeccionRequest;
import com.SICOIL.dtos.capital.CapitalMovimientoFiltro;
import com.SICOIL.dtos.capital.CapitalMovimientoResponse;
import com.SICOIL.dtos.capital.CapitalResumenResponse;
import com.SICOIL.models.CapitalOrigen;
import com.SICOIL.services.capital.CapitalService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/capital")
@RequiredArgsConstructor
public class CapitalController {

    private final CapitalService capitalService;

    @GetMapping("/movimientos")
    public ResponseEntity<Page<CapitalMovimientoResponse>> listarMovimientos(
            @RequestParam(required = false) CapitalOrigen origen,
            @RequestParam(required = false) Boolean esCredito,
            @RequestParam(required = false) Long referenciaId,
            @RequestParam(required = false) String descripcion,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        CapitalMovimientoFiltro filtro = CapitalMovimientoFiltro.builder()
                .origen(origen)
                .esCredito(esCredito)
                .referenciaId(referenciaId)
                .descripcion(descripcion)
                .desde(desde)
                .hasta(hasta)
                .build();

        Page<CapitalMovimientoResponse> pagina = capitalService.obtenerMovimientos(filtro, pageable);
        return ResponseEntity.ok(pagina);
    }

    @GetMapping("/resumen")
    public ResponseEntity<CapitalResumenResponse> obtenerResumen(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        CapitalResumenResponse response = capitalService.obtenerResumen(desde, hasta);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/inyecciones")
    public ResponseEntity<CapitalMovimientoResponse> registrarInyeccion(
            @Valid @RequestBody CapitalInyeccionRequest request
    ) {
        CapitalMovimientoResponse response = capitalService.registrarInyeccionCapital(
                request.getMonto(),
                request.getDescripcion(),
                request.getFechaRegistro()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/retiros")
    public ResponseEntity<CapitalMovimientoResponse> registrarRetiroGanancia(
            @Valid @RequestBody CapitalInyeccionRequest request
    ) {
        CapitalMovimientoResponse response = capitalService.registrarRetiroCapital(
                request.getMonto(),
                request.getDescripcion(),
                request.getFechaRegistro()
        );
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
