package com.SICOIL.repositories;

import com.SICOIL.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    Optional<Producto> findById(Long id);

    //consulta para traer una lista de los nombres sin repetir para usar al momento de crear un registro, es solo informativo
    @Query("SELECT DISTINCT p.nombre FROM Producto p")
    List<String> findDistinctNombres();

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Producto> findByNombreIgnoreCase(String nombre);

    @Query("SELECT p.nombre AS nombre, SUM(p.stock) AS stockTotal, MAX(p.cantidadPorCajas) AS cantidadPorCajas " +
            "FROM Producto p GROUP BY p.nombre")
    List<ProductosSInPrecio> inventarioAgrupado();
}
