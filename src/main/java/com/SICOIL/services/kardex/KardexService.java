package com.SICOIL.services.kardex;

import com.SICOIL.dtos.kardex.KardexResponse;
import com.SICOIL.mappers.kardex.KardexMapper;
import com.SICOIL.models.Kardex;
import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.models.Producto;
import com.SICOIL.repositories.KardexRepository;
import com.SICOIL.services.usuario.UsuarioService;
import java.time.LocalDate;
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

    @Transactional(readOnly = true)
    public Page<KardexResponse> buscar(
            Pageable pageable,
            Long productoId,
            Long usuarioId,
            MovimientoTipo tipo,
            LocalDate desde,
            LocalDate hasta
    ) {
        Specification<Kardex> spec = KardexSpecification.filtros(productoId, usuarioId, tipo, desde, hasta);

        log.debug("Buscando movimientos de kardex con filtros productoId={}, usuarioId={}, tipo={}, desde={}, hasta={}",
                productoId, usuarioId, tipo, desde, hasta);
        return kardexRepository.findAll(spec, pageable)
                .map(kardexMapper::entityToResponse);
    }

    public Kardex registrarMovimiento(Producto producto,
                                      Integer cantidad,
                                      String comentario,
                                      MovimientoTipo tipo) {

        log.info("Registrando movimiento en kardex. Producto={} tipo={} cantidad={}", producto.getId(), tipo, cantidad);
        Kardex movimiento = Kardex.builder()
                .producto(producto)
                .usuario(usuarioService.obtenerUsuarioActual())
                .cantidad(cantidad)
                .tipo(tipo)
                .comentario(comentario)
                .build();

        return kardexRepository.save(movimiento);
    }
}
