package com.SICOIL.services.producto;

import com.SICOIL.dtos.producto.*;
import com.SICOIL.mappers.producto.ProductoMapper;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.services.InventarioService;
import jakarta.persistence.EntityNotFoundException;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;
    private final InventarioService inventarioService;

    @Transactional(readOnly = true)
    public PaginaProductoResponse buscar(
            String nombreFiltro,
            int page,
            int size
    ) {

        // 1. Filtro con Specification
        Specification<Producto> spec =
                Specification.where(ProductoSpecification.hasNombre(nombreFiltro));

        List<Producto> productos = productoRepository.findAll(spec);

        // 2. Agrupar productos por nombre
        Map<String, List<Producto>> grupos = productos.stream()
                .collect(Collectors.groupingBy(Producto::getNombre));

        // 3. Convertir cada grupo en DTO padre (plegable)
        List<ProductosAgrupadosResponse> listaCompleta = grupos.entrySet().stream()
                .map(entry -> {
                    String nombre = entry.getKey();
                    List<Producto> variantes = entry.getValue();

                    ProductosAgrupadosResponse dto = new ProductosAgrupadosResponse();
                    dto.setNombre(nombre);

                    dto.setStockTotal(
                            variantes.stream()
                                    .mapToInt(p -> p.getStock() != null ? p.getStock() : 0)
                                    .sum()
                    );

                    dto.setCantidadPorCajas(
                            variantes.stream()
                                    .map(Producto::getCantidadPorCajas)
                                    .filter(Objects::nonNull)
                                    .findFirst()
                                    .orElse(0)
                    );

                    dto.setVariantes(
                            variantes.stream()
                                    .map(p -> {
                                        ProductosDesagrupadosResponse v = new ProductosDesagrupadosResponse();
                                        v.setId(p.getId());
                                        v.setPrecioCompra(p.getPrecioCompra());
                                        v.setStock(p.getStock());
                                        return v;
                                    })
                                    .toList()
                    );

                    return dto;
                })
                .sorted(Comparator.comparing(ProductosAgrupadosResponse::getNombre))
                .toList();

        // 4. PAGINACIÓN EN MEMORIA
        int totalElements = listaCompleta.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;
        int toIndex = Math.min(fromIndex + size, totalElements);

        List<ProductosAgrupadosResponse> pagina =
                fromIndex < totalElements ? listaCompleta.subList(fromIndex, toIndex) : List.of();

        // 5. Construir respuesta final
        PaginaProductoResponse response = new PaginaProductoResponse();
        response.setContent(pagina);
        response.setPage(page);
        response.setSize(size);
        response.setTotalElements(totalElements);
        response.setTotalPages(totalPages);

        return response;
    }



    public ProductoResponse crearProducto(ProductoRequest productoRequest) {

        log.info("Creando producto '{}'", productoRequest.getNombre());

        if (productoRepository.existsByNombreIgnoreCase(productoRequest.getNombre())) {
            log.warn("Intento de crear producto duplicado '{}'", productoRequest.getNombre());
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + productoRequest.getNombre());
        }

        Producto producto = productoMapper.requestToEntity(productoRequest);
        Producto guardado = productoRepository.save(producto);

        if (guardado.getStock() != null && guardado.getStock() > 0) {
            log.debug("Registrando stock inicial para producto {} con cantidad {}", guardado.getId(), guardado.getStock());
            inventarioService.registrarStockInicial(guardado, "Stock inicial del producto");
        }

        return productoMapper.entitytoResponse(guardado);
    }

    public ProductoResponse actualizarProducto(Long id, ProductoRequest productoRequest) {
        log.info("Actualizando producto con id {}", id);

        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));

        if (!Objects.equals(producto.getStock(), productoRequest.getStock())) {
            log.warn("Intento de modificar stock desde actualizar producto para id {}", id);
            throw new IllegalArgumentException("No está permitido cambiar el stock desde actualizar producto");
        }

        productoRepository.findByNombreIgnoreCase(productoRequest.getNombre())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Ya existe un producto con el nombre: " + productoRequest.getNombre());
                });

        productoMapper.updateEntityFromRequest(productoRequest, producto);

        Producto guardado = productoRepository.save(producto);
        return productoMapper.entitytoResponse(guardado);
    }

    public ProductoResponse agregarCantidadPrecioExistente(Long id, Integer cantidad, String observacion) {
        log.info("Agregando stock a producto {} con cantidad {}", id, cantidad);
        Producto actualizado = inventarioService.registrarEntradaExistente(id, cantidad, observacion);
        return productoMapper.entitytoResponse(actualizado);
    }

    public ProductoResponse agregarCantidadPrecioNuevo(Long id, Integer cantidad, Double precioNuevo, String observacion) {
        log.info("Agregando stock con nuevo precio al producto {} cantidad {} precio {}", id, cantidad, precioNuevo);
        Producto actualizado = inventarioService.registrarEntradaNuevoPrecio(id, cantidad, precioNuevo, observacion);
        return productoMapper.entitytoResponse(actualizado);
    }

    public ProductoResponse eliminarCantidad(Long id, Integer cantidad, String observacion) {
        log.info("Eliminando stock del producto {} cantidad {}", id, cantidad);
        Producto actualizado = inventarioService.registrarSalida(id, cantidad, observacion);
        return productoMapper.entitytoResponse(actualizado);
    }
}
