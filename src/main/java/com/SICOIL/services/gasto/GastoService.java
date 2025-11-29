//package com.SICOIL.services.gasto;
//
//import static com.SICOIL.services.gasto.GastoSpecification.descripcionContains;
//import static com.SICOIL.services.gasto.GastoSpecification.fechaBetween;
//
//import com.SICOIL.models.Compra;
//import com.SICOIL.repositories.GastoRepository;
//import jakarta.persistence.EntityNotFoundException;
//import java.time.LocalDate;
//import java.util.List;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Sort;
//import org.springframework.data.jpa.domain.Specification;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
//@Service
//@Transactional
//public class GastoService {
//
//    @Autowired
//    private GastoRepository gastoRepository;
//
//    @Transactional(readOnly = true)
//    public List<Compra> listar(LocalDate desde, LocalDate hasta, String nombre) {
//        boolean sinFiltros = (nombre == null || nombre.isBlank()) && desde == null && hasta == null;
//        if (sinFiltros) {
//            desde = LocalDate.now();
//            hasta = LocalDate.now();
//        }
//
//        Specification<Compra> spec = Specification.where(descripcionContains(nombre))
//                .and(fechaBetween(desde, hasta));
//
//        return gastoRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fecha").and(Sort.by("descripcion")));
//    }
//
//    @Transactional(readOnly = true)
//    public List<Compra> listar() {
//        return listar(null, null, null);
//    }
//
//    @Transactional(readOnly = true)
//    public Compra obtenerPorId(Long id) {
//        return gastoRepository.findById(id)
//                .orElseThrow(() -> new EntityNotFoundException("Compra no encontrado con id: " + id));
//    }
//
//    public Compra crear(Compra compra) {
//        compra.setId(null);
//        return gastoRepository.save(compra);
//    }
//
//    public Compra actualizar(Long id, Compra cambios) {
//        Compra compra = obtenerPorId(id);
//        compra.setMonto(cambios.getMonto());
//        compra.setDescripcion(cambios.getDescripcion());
//        return gastoRepository.save(compra);
//    }
//
//    public void eliminar(Long id) {
//        Compra compra = obtenerPorId(id);
//        gastoRepository.delete(compra);
//    }
//}
