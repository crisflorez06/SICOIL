package com.SICOIL.repositories;

import com.SICOIL.models.CarteraMovimiento;
import com.SICOIL.models.CarteraMovimientoTipo;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CarteraMovimientoRepository extends JpaRepository<CarteraMovimiento, Long>,
        JpaSpecificationExecutor<CarteraMovimiento> {

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CarteraMovimiento cm
            where cm.tipo = com.SICOIL.models.CarteraMovimientoTipo.ABONO
              and (:inicio is null or cm.fecha >= :inicio)
              and (:fin is null or cm.fecha <= :fin)
            """)
    Double sumAbonosBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CarteraMovimiento cm
            where cm.tipo = com.SICOIL.models.CarteraMovimientoTipo.CREDITO
              and cm.cartera.venta.activa = true
              and (:inicio is null or cm.fecha >= :inicio)
              and (:fin is null or cm.fecha <= :fin)
            """)
    Double sumCreditosBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    boolean existsByCarteraVentaIdAndTipo(Long ventaId, CarteraMovimientoTipo tipo);
}
