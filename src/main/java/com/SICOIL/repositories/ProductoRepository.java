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

    Optional<Producto> findByNombreIgnoreCase(String nombre);

    @Query("SELECT p.nombre AS nombre, SUM(p.stock) AS stockTotal, MAX(p.cantidadPorCajas) AS cantidadPorCajas " +
            "FROM Producto p GROUP BY p.nombre")
    List<ProductosSInPrecio> inventarioAgrupado();

    @Query("SELECT p.id AS id, p.precioCompra AS precioCompra FROM Producto p WHERE LOWER(p.nombre) = LOWER(:nombre)")
    List<ProductoIdPrecio> findIdAndPrecioByNombre(@org.springframework.data.repository.query.Param("nombre") String nombre);

    Optional<Producto> findFirstByNombreIgnoreCase(String nombre);

    List<Producto> findAllByNombreIgnoreCase(String nombre);

}
