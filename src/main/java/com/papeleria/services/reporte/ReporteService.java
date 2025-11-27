package com.papeleria.services.reporte;

import com.papeleria.dtos.ProductoMasVendidoResponse;
import com.papeleria.dtos.ProductoResponse;
import com.papeleria.dtos.ReporteGeneralResponse;
import com.papeleria.mappers.producto.ProductoMapper;
import com.papeleria.models.Producto;
import com.papeleria.repositories.GastoRepository;
import com.papeleria.repositories.ProductoRepository;
import com.papeleria.repositories.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private static final int LIMITE_MAS_VENDIDOS = 10;
    private static final int UMBRAL_STOCK_BAJO_DEFAULT = 3;

    private final VentaRepository ventaRepository;
    private final GastoRepository gastoRepository;
    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public ReporteGeneralResponse getReporteGeneral(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDateTime inicio = fechaInicio.atStartOfDay();
        LocalDateTime fin = fechaFin.atTime(LocalTime.MAX);

        BigDecimal totalGanancias = ventaRepository.sumGananciaByFechaBetween(inicio, fin);
        BigDecimal totalDineroEnVentas = ventaRepository.sumTotalByFechaBetween(inicio, fin);
        BigDecimal totalGastos = gastoRepository.sumMontoByFechaBetween(fechaInicio, fechaFin);
        BigDecimal totalGananciasNetas = totalGanancias.subtract(totalGastos);
        Long totalVentas = ventaRepository.countByFechaBetween(inicio, fin);
        List<ProductoMasVendidoResponse> productosMasVendidos = obtenerProductosMasVendidos(inicio, fin);

        return ReporteGeneralResponse.builder()
            .totalGanancias(totalGananciasNetas)
            .totalGastos(totalGastos)
            .totalDineroEnVentas(totalDineroEnVentas)
            .totalVentas(totalVentas)
            .productosMasVendidos(productosMasVendidos)
            .build();
    }

    public List<ProductoResponse> getProductosConStockBajo(Optional<Integer> umbral) {
        int umbralStock = umbral.orElse(UMBRAL_STOCK_BAJO_DEFAULT);
        List<Producto> productos = productoRepository.findByStockLessThanEqual(umbralStock);
        return productoMapper.toResponseList(productos);
    }

    private List<ProductoMasVendidoResponse> obtenerProductosMasVendidos(LocalDateTime inicio, LocalDateTime fin) {
        return ventaRepository.findTopSellingProductsByFechaBetween(inicio, fin, PageRequest.of(0, LIMITE_MAS_VENDIDOS)).stream()
            .map(this::mapProductoMasVendido)
            .collect(Collectors.toList());
    }

    private ProductoMasVendidoResponse mapProductoMasVendido(Object[] fila) {
        Long productoId = (Long) fila[0];
        String nombre = (String) fila[1];
        Long cantidadVendida = fila[2] != null ? ((Number) fila[2]).longValue() : 0L;
        BigDecimal totalGenerado = fila[3] != null ? (BigDecimal) fila[3] : BigDecimal.ZERO;
        return ProductoMasVendidoResponse.builder()
            .productoId(productoId)
            .nombre(nombre)
            .cantidadVendida(cantidadVendida)
            .totalGenerado(totalGenerado)
            .build();
    }
}
