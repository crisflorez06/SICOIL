/*
package com.SICOIL.repositories;

import com.SICOIL.models.Compra;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepository extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    @Query("select coalesce(sum(c.total), 0) from Compra c where c.fechaRegistro between :inicio and :fin")
    BigDecimal sumMontoByFechaBetween(
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}
*/
