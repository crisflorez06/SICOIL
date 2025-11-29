//package com.SICOIL.controllers;
//
//import com.SICOIL.dtos.filtro.FiltrosDTO;
//import com.SICOIL.services.FiltroService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//@RequestMapping("/api/filtros")
//public class FiltroController {
//
//    @Autowired
//    private FiltroService filtroService;
//
//    @GetMapping
//    public ResponseEntity<FiltrosDTO> getFiltros() {
//        return ResponseEntity.ok(filtroService.getFiltros());
//    }
//}
