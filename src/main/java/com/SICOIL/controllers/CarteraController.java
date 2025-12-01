package com.SICOIL.controllers;

import com.SICOIL.dtos.cartera.CarteraAbonoDetalleResponse;
import com.SICOIL.dtos.cartera.CarteraCreditoDetalleResponse;
import com.SICOIL.dtos.cartera.CarteraAbonoRequest;
import com.SICOIL.dtos.cartera.CarteraResumenResponse;
import com.SICOIL.services.cartera.CarteraService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cartera")
@RequiredArgsConstructor
public class CarteraController {

    private final CarteraService carteraService;

    @GetMapping("/pendientes")
    public ResponseEntity<List<CarteraResumenResponse>> listarPendientes(
            @RequestParam(required = false) String cliente,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        List<CarteraResumenResponse> response = carteraService.listarPendientes(cliente, desde, hasta);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/clientes/{clienteId}/abonos")
    public ResponseEntity<List<CarteraAbonoDetalleResponse>> listarAbonos(
            @PathVariable Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        List<CarteraAbonoDetalleResponse> response = carteraService.listarAbonos(clienteId, desde, hasta);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/clientes/{clienteId}/creditos")
    public ResponseEntity<List<CarteraCreditoDetalleResponse>> listarCreditos(
            @PathVariable Long clienteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        List<CarteraCreditoDetalleResponse> response = carteraService.listarCreditos(clienteId, desde, hasta);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/clientes/{clienteId}/abonos")
    public ResponseEntity<List<CarteraAbonoDetalleResponse>> registrarAbono(
            @PathVariable Long clienteId,
            @Valid @RequestBody CarteraAbonoRequest request
    ) {
        List<CarteraAbonoDetalleResponse> response = carteraService.registrarAbono(clienteId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}
