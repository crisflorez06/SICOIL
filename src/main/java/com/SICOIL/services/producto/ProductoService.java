package com.SICOIL.services.producto;

import com.SICOIL.dtos.producto.*;
import com.SICOIL.mappers.producto.ProductoMapper;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.services.InventarioService;
import jakarta.persistence.EntityNotFoundException;

import java.util.*;
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


    /**
     * Obtiene un producto por su identificador único.
     * Este método se utiliza durante el proceso de registro de una venta para
     * recuperar información del producto y reutilizarla en operaciones posteriores.
     *
     * @param id identificador del producto que se desea consultar
     * @return la entidad {@link Producto} correspondiente al identificador proporcionado
     * @throws EntityNotFoundException si no existe un producto con el identificador especificado
     */
    @Transactional(readOnly = true)
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado con id: " + id));
    }


    /**
     * Obtiene una lista paginada de productos, permitiendo aplicar un filtro opcional por nombre.
     * Los productos recuperados desde la base de datos se agrupan por nombre para construir una vista
     * de dos niveles utilizada en el frontend:
     *
     * <p><b>1. Vista principal (nivel superior):</b><br>
     *     Muestra información general del producto agrupado por nombre, incluyendo:
     *     total de stock disponible, cantidad por cajas y el nombre del producto.
     *
     * <p><b>2. Vista desplegable (nivel inferior):</b><br>
     *     Permite visualizar todas las variantes del producto que comparten el mismo nombre,
     *     pero difieren en atributos como precio de compra y stock. Cada variante se muestra
     *     individualmente para permitir operaciones específicas como seleccionar una variante
     *     para ventas u otros procesos.
     *
     * <p>Además, el método realiza:
     * <ul>
     *   <li>Aplicación de un filtro dinámico mediante {@link Specification}.</li>
     *   <li>Agrupación de productos por nombre.</li>
     *   <li>Construcción de DTOs para la vista superior e inferior.</li>
     *   <li>Paginación en memoria de los grupos resultantes.</li>
     * </ul>
     *
     * @param nombreFiltro nombre parcial o completo del producto utilizado como filtro;
     *                     puede ser {@code null} o vacío para obtener todos los registros
     * @param page número de página solicitada (basado en 0)
     * @param size cantidad de elementos por página
     * @return una instancia de {@link PaginaProductoResponse} con los productos agrupados listos para ser mostrados en la vista
     */
    @Transactional(readOnly = true)
    public PaginaProductoResponse traerTodos(
            String nombreFiltro,
            int page,
            int size
    ) {

        log.debug("Listando productos con filtro='{}' page={} size={}", nombreFiltro, page, size);
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


    /**
     * Crea un nuevo producto en el sistema y registra su stock inicial mediante el servicio de inventario.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validación de que no exista otro producto con el mismo nombre (ignorando mayúsculas/minúsculas).</li>
     *   <li>Conversión del {@link ProductoRequest} a entidad mediante el mapper correspondiente.</li>
     *   <li>Persistencia del producto en la base de datos.</li>
     *   <li>Registro del stock inicial si el producto fue creado con una cantidad mayor a cero,
     *       delegando dicha operación al servicio de inventario.</li>
     * </ul>
     *
     * @param productoRequest información necesaria para crear el producto
     * @return un {@link ProductoResponse} que representa el producto creado
     * @throws IllegalArgumentException si ya existe un producto con el mismo nombre
     */
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

    /**
     * Actualiza el nombre y la cantidad por cajas de todas las variantes que comparten el mismo
     * nombre que el producto seleccionado. Esta operación permite mantener sincronizados los
     * datos comunes de un grupo de productos sin alterar precios ni stock individuales.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Validar que exista al menos una variante con el nombre recibido.</li>
     *   <li>Verificar que el nuevo nombre no esté siendo utilizado por otro grupo distinto.</li>
     *   <li>Buscar todas las variantes que comparten el mismo nombre original.</li>
     *   <li>Actualizar en cada variante el nuevo nombre y la cantidad por cajas.</li>
     *   <li>Persistir los cambios en lote y confirmar la operación.</li>
     * </ul>
     *
     * @param nombreAnterior nombre actual que comparten las variantes a actualizar
     * @param request datos con el nuevo nombre y la cantidad por cajas deseada
     * @return {@code true} si al menos una variante fue actualizada
     * @throws EntityNotFoundException si no existen productos con el nombre indicado
     * @throws IllegalArgumentException si el nuevo nombre ya está asignado a otro grupo distinto
     */
    public boolean actualizarProducto(String nombreAnterior, ProductoActualizarRequest request) {
        if (nombreAnterior == null || nombreAnterior.isBlank()) {
            throw new IllegalArgumentException("El nombre anterior es obligatorio para actualizar el producto.");
        }

        log.info("Actualizando grupo de productos con nombre '{}'", nombreAnterior);

        List<Producto> variantes = productoRepository.findAllByNombreIgnoreCase(nombreAnterior);
        if (variantes.isEmpty()) {
            throw new EntityNotFoundException("No se encontraron productos con nombre: " + nombreAnterior);
        }

        String nombreActual = variantes.get(0).getNombre();
        String nuevoNombre = request.getNombre().trim();

        if (!nombreActual.equalsIgnoreCase(nuevoNombre)
                && productoRepository.existsByNombreIgnoreCase(nuevoNombre)) {
            throw new IllegalArgumentException("Ya existe un producto con el nombre: " + nuevoNombre);
        }

        for (Producto variante : variantes) {
            variante.setNombre(nuevoNombre);
            variante.setCantidadPorCajas(request.getCantidadPorCajas());
        }

        productoRepository.saveAll(variantes);
        return true;
    }

    /**
     * Registra el ingreso de múltiples productos al inventario.
     * Para cada elemento de la lista, se delega al servicio de inventario la lógica
     * de ingreso, la cual determina si debe:
     * <ul>
     *   <li>Crear una nueva variante del producto cuando el precio de compra no coincide con
     *       ninguna variante existente que comparta el mismo nombre.</li>
     *   <li>Actualizar el stock de una variante existente cuando ya existe un producto con
     *       el mismo nombre y el mismo precio.</li>
     * </ul>
     *
     * <p>Cada producto procesado es convertido a un {@link ProductoResponse} para su retorno.</p>
     *
     * @param lista lista de solicitudes de ingreso representadas por {@link IngresoProductoRequest}
     * @return lista de {@link ProductoResponse} que refleja las variantes creadas o actualizadas
     */
    @Transactional
    public List<ProductoResponse> registrarIngresoProductos(List<IngresoProductoRequest> lista) {

        List<ProductoResponse> respuestas = new ArrayList<>();

        for (IngresoProductoRequest req : lista) {
            Producto actualizado = inventarioService.registrarIngresoProducto(req);
            respuestas.add(productoMapper.entitytoResponse(actualizado));
        }

        return respuestas;
    }


    /**
     * Elimina una cantidad específica del stock de un producto existente y delega al
     * servicio de inventario la actualización correspondiente.
     * Este proceso ajusta el stock realizando una salida controlada y permite
     * registrar una observación asociada a la operación para fines de trazabilidad.
     *
     * @param id identificador del producto al cual se le descontará stock
     * @param cantidad cantidad de unidades que se desea eliminar del inventario
     * @param observacion detalle o motivo de la eliminación del stock
     * @return un {@link ProductoResponse} que representa el estado actualizado del producto
     */
    public ProductoResponse eliminarCantidad(Long id, Integer cantidad, String observacion) {
        log.info("Eliminando stock del producto {} cantidad {}", id, cantidad);
        Producto actualizado = inventarioService.registrarSalida(id, cantidad, observacion);
        return productoMapper.entitytoResponse(actualizado);
    }

}
