package com.SICOIL.repositories;

import com.SICOIL.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findById(Long id);

    boolean existsByNombreIgnoreCase(String nombre);

    List<Producto> findByNombreIgnoreCaseOrderByFechaRegistroAsc(String nombre);

    @Query("SELECT p.nombre AS nombre, SUM(p.stock) AS stockTotal, MAX(p.cantidadPorCajas) AS cantidadPorCajas " +
            "FROM Producto p GROUP BY p.nombre")
    List<ProductosSInPrecio> inventarioAgrupado();

    @Query("""
            select coalesce(sum(coalesce(p.stock, 0) * coalesce(p.precioCompra, 0)), 0)
            from Producto p
            """)
    Double sumValorInventario();

    Optional<Producto> findFirstByNombreIgnoreCase(String nombre);

    List<Producto> findAllByNombreIgnoreCase(String nombre);

    List<Producto> findByNombreOrderByFechaRegistroAsc(String nombre);
}
