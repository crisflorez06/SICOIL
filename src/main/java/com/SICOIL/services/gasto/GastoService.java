package com.SICOIL.services.gasto;

import static com.SICOIL.services.gasto.GastoSpecification.descripcionContains;
import static com.SICOIL.services.gasto.GastoSpecification.fechaBetween;

import com.SICOIL.models.Gasto;
import com.SICOIL.repositories.GastoRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Transactional(readOnly = true)
    public List<Gasto> listar(LocalDate desde, LocalDate hasta, String nombre) {
        boolean sinFiltros = (nombre == null || nombre.isBlank()) && desde == null && hasta == null;
        if (sinFiltros) {
            desde = LocalDate.now();
            hasta = LocalDate.now();
        }

        Specification<Gasto> spec = Specification.where(descripcionContains(nombre))
                .and(fechaBetween(desde, hasta));

        return gastoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by("descripcion")));
    }

    @Transactional(readOnly = true)
    public List<Gasto> listar() {
        return listar(null, null, null);
    }

    @Transactional(readOnly = true)
    public Gasto obtenerPorId(Long id) {
        return gastoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Gasto no encontrado con id: " + id));
    }

    public Gasto crear(Gasto gasto) {
        gasto.setId(null);
        return gastoRepository.save(gasto);
    }

    public Gasto actualizar(Long id, Gasto cambios) {
        Gasto gasto = obtenerPorId(id);
        gasto.setMonto(cambios.getMonto());
        gasto.setDescripcion(cambios.getDescripcion());
        return gastoRepository.save(gasto);
    }

    public void eliminar(Long id) {
        Gasto gasto = obtenerPorId(id);
        gastoRepository.delete(gasto);
    }
}
