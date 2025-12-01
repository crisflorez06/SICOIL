package com.SICOIL.repositories;

import com.SICOIL.models.DetalleVenta;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long>, JpaSpecificationExecutor<DetalleVenta> {

    @Query("select dv from DetalleVenta dv where date(dv.venta.fechaRegistro) = :fechaActual")
    List<DetalleVenta> findAllByVentaFecha(@Param("fechaActual") LocalDate fechaActual);

    List<DetalleVenta> findByVentaId(Long ventaId);
}
