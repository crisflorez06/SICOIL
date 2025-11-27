package com.SICOIL.repositories;

import com.SICOIL.models.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    List<Producto> findByStockLessThanEqual(Integer stock);

    @Query("SELECT DISTINCT p.nombre FROM Producto p")
    List<String> findDistinctNombres();

    @Query("SELECT DISTINCT p.categoria FROM Producto p")
    List<String> findDistinctCategoria();

    boolean existsByNombreIgnoreCase(String nombre);

    Optional<Producto> findByNombreIgnoreCase(String nombre);
}
