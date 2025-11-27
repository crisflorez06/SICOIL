package com.SICOIL.repositories;

import com.SICOIL.models.DetalleVenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long>, JpaSpecificationExecutor<DetalleVenta> {

    @Query("SELECT dv FROM DetalleVenta dv WHERE DATE(dv.venta.fecha) = :fechaActual")
    List<DetalleVenta> findAllByVentaFecha(@Param("fechaActual") LocalDate fechaActual);

    List<DetalleVenta> findByVentaId(Long ventaId);
}
