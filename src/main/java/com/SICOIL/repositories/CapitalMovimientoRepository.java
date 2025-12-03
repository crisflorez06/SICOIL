package com.SICOIL.repositories;

import com.SICOIL.models.CapitalMovimiento;
import com.SICOIL.models.CapitalOrigen;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CapitalMovimientoRepository extends JpaRepository<CapitalMovimiento, Long>,
        JpaSpecificationExecutor<CapitalMovimiento> {

    List<CapitalMovimiento> findByOrigenAndReferenciaId(CapitalOrigen origen, Long referenciaId);

    List<CapitalMovimiento> findByCreadoEnBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("select coalesce(sum(cm.monto), 0) from CapitalMovimiento cm where cm.esCredito = false")
    Double sumMontoReal();

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CapitalMovimiento cm
            where cm.esCredito = false and cm.monto > 0
            """)
    Double sumEntradas();

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CapitalMovimiento cm
            where cm.esCredito = false and cm.monto < 0
            """)
    Double sumSalidas();

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CapitalMovimiento cm
            where cm.esCredito = true
            """)
    Double sumTotalCreditos();

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CapitalMovimiento cm
            where cm.esCredito = false
              and (:inicio is null or cm.creadoEn >= :inicio)
              and (:fin is null or cm.creadoEn <= :fin)
            """)
    Double sumMontoRealBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CapitalMovimiento cm
            where cm.esCredito = false and cm.monto > 0
              and (:inicio is null or cm.creadoEn >= :inicio)
              and (:fin is null or cm.creadoEn <= :fin)
            """)
    Double sumEntradasBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select coalesce(sum(cm.monto), 0)
            from CapitalMovimiento cm
            where cm.esCredito = false and cm.monto < 0
              and (:inicio is null or cm.creadoEn >= :inicio)
              and (:fin is null or cm.creadoEn <= :fin)
            """)
    Double sumSalidasBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);
}
