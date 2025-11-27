package com.papeleria.repositories;

import com.papeleria.models.Gasto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepository extends JpaRepository<Gasto, Long>, JpaSpecificationExecutor<Gasto> {

    @Query("select coalesce(sum(g.monto), 0) from Gasto g where g.fecha between :inicio and :fin")
    BigDecimal sumMontoByFechaBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}
