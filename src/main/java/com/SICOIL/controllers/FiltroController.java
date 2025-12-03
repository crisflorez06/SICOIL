package com.SICOIL.controllers;

import com.SICOIL.dtos.filtro.FiltrosResponse;
import com.SICOIL.services.filtro.FiltroService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/filtros")
@RequiredArgsConstructor
public class FiltroController {

    private final FiltroService filtroService;

    @GetMapping
    public ResponseEntity<FiltrosResponse> obtenerFiltros() {
        return ResponseEntity.ok(filtroService.obtenerFiltros());
    }
}

