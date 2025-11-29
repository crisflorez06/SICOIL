package com.SICOIL.controllers;


import com.SICOIL.dtos.kardex.KardexResponse;
import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.services.kardex.KardexService;
import java.time.LocalDate;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/kardex")
@RequiredArgsConstructor
public class KardexController {


    private final KardexService kardexService;


    @GetMapping
    public ResponseEntity<?> listarKardex(
            @PageableDefault(size = 20, sort = "fechaRegistro", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) MovimientoTipo tipo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {

        Page<KardexResponse> movimientos =
                kardexService.buscar(pageable, productoId, usuarioId, tipo, desde, hasta);

        if (movimientos.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(movimientos);
    }

}

