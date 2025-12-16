package com.SICOIL.services.kardex;

import com.SICOIL.dtos.kardex.KardexResponse;
import com.SICOIL.mappers.kardex.KardexMapper;
import com.SICOIL.models.Kardex;
import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.KardexRepository;
import com.SICOIL.services.usuario.UsuarioService;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Slf4j
public class KardexService {

    private final KardexRepository kardexRepository;
    private final UsuarioService usuarioService;
    private final KardexMapper kardexMapper;

    /**
     * Recupera una lista paginada de movimientos registrados en el kardex,
     * permitiendo la aplicación de varios filtros opcionales como producto,
     * usuario responsable, tipo de movimiento y rango de fechas.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Construir dinámicamente una {@link Specification} a partir de los filtros recibidos.</li>
     *   <li>Consultar la base de datos con paginación según el {@link Pageable} proporcionado.</li>
     *   <li>Convertir cada resultado a {@link KardexResponse} mediante el mapper correspondiente.</li>
     * </ul>
     *
     * @param pageable parámetros de paginación y ordenamiento
     * @param productoId identificador del producto a filtrar; puede ser {@code null}
     * @param usuarioId identificador del usuario que registró el movimiento; puede ser {@code null}
     * @param nombreProducto fragmento del nombre del producto para filtrar (insensible a mayúsculas); puede ser {@code null}
     * @param tipo tipo de movimiento (ENTRADA, SALIDA); puede ser {@code null}
     * @param desde fecha inicial del rango a consultar; puede ser {@code null}
     * @param hasta fecha final del rango a consultar; puede ser {@code null}
     * @return una página de {@link KardexResponse} con los movimientos encontrados según los filtros aplicados
     */
    @Transactional(readOnly = true)
    public Page<KardexResponse> buscar(
            Pageable pageable,
            Long productoId,
            Long usuarioId,
            String nombreProducto,
            MovimientoTipo tipo,
            LocalDate desde,
            LocalDate hasta
    ) {
        Specification<Kardex> spec = KardexSpecification.filtros(productoId, usuarioId, nombreProducto, tipo, desde, hasta);

        log.debug("Buscando movimientos de kardex con filtros productoId={}, usuarioId={}, nombreProducto={}, tipo={}, desde={}, hasta={}",
                productoId, usuarioId, nombreProducto, tipo, desde, hasta);
        return kardexRepository.findAll(spec, pageable)
                .map(kardexMapper::entityToResponse);
    }

    /**
     * Registra un movimiento de inventario en el kardex asociado a un producto,
     * especificando la cantidad, el tipo de movimiento y un comentario opcional.
     * El usuario responsable del movimiento se obtiene automáticamente del
     * contexto de autenticación.
     *
     * <p>El proceso incluye:
     * <ul>
     *   <li>Registrar en logs los datos del movimiento (producto, tipo y cantidad).</li>
     *   <li>Construir una entidad {@link Kardex} con la información del producto,
     *       usuario actual, cantidad, tipo de movimiento y comentario.</li>
     *   <li>Persistir el movimiento en la base de datos.</li>
     * </ul>
     *
     * @param producto producto sobre el que se realiza el movimiento
     * @param cantidad cantidad de unidades afectadas por el movimiento
     * @param comentario descripción o motivo del movimiento; puede ser {@code null}
     * @param tipo tipo de movimiento registrado (ENTRADA o SALIDA)
     * @return la entidad {@link Kardex} creada y almacenada en la base de datos
     */
    public Kardex registrarMovimiento(Producto producto,
                                      Integer cantidad,
                                      String comentario,
                                      MovimientoTipo tipo,
                                      LocalDateTime fecha) {

        log.info("Registrando movimiento en kardex. Producto={} tipo={} cantidad={}", producto.getId(), tipo, cantidad);
        Kardex movimiento = Kardex.builder()
                .producto(producto)
                .usuario(usuarioService.obtenerUsuarioActual())
                .cantidad(cantidad)
                .tipo(tipo)
                .comentario(comentario)
                .fechaRegistro(fecha)
                .build();

        return kardexRepository.save(movimiento);
    }
}
