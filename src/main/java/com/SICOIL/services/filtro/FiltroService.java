package com.SICOIL.services.filtro;

import com.SICOIL.dtos.filtro.FiltroClienteResponse;
import com.SICOIL.dtos.filtro.FiltroPrecioResponse;
import com.SICOIL.dtos.filtro.FiltroProductoResponse;
import com.SICOIL.dtos.filtro.FiltrosResponse;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.ClienteRepository;
import com.SICOIL.repositories.ProductoRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FiltroService {

    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;

    public FiltrosResponse obtenerFiltros() {
        return FiltrosResponse.builder()
                .productos(obtenerProductosAgrupados())
                .clientes(obtenerClientes())
                .build();
    }

    private List<FiltroProductoResponse> obtenerProductosAgrupados() {
        List<Producto> productos = productoRepository.findAll();
        log.debug("Construyendo filtros de productos para {} registros", productos.size());

        Map<String, List<Producto>> agrupados = productos.stream()
                .collect(Collectors.groupingBy(Producto::getNombre));

        return agrupados.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .map(entry -> {
                    List<Producto> variantes = entry.getValue();
                    Integer cantidadPorCajas = variantes.stream()
                            .map(Producto::getCantidadPorCajas)
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null);

                    return FiltroProductoResponse.builder()
                            .nombreProducto(entry.getKey())
                            .cantidadPorCajas(cantidadPorCajas)
                            .precios(variantes.stream()
                                    .sorted(Comparator.comparing(Producto::getId))
                                    .map(producto -> FiltroPrecioResponse.builder()
                                            .id(producto.getId())
                                            .precioCompra(producto.getPrecioCompra())
                                            .cantidad(producto.getStock())
                                            .build())
                                    .toList())
                            .build();
                })
                .toList();
    }

    private List<FiltroClienteResponse> obtenerClientes() {
        List<Cliente> clientes = clienteRepository.findAll(Sort.by(Sort.Direction.ASC, "nombre"));
        log.debug("Construyendo filtros de clientes para {} registros", clientes.size());

        return clientes.stream()
                .map(cliente -> FiltroClienteResponse.builder()
                        .id(cliente.getId())
                        .nombre(cliente.getNombre())
                        .build())
                .toList();
    }
}
