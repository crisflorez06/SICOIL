package com.SICOIL.repositories;

import com.SICOIL.models.Cartera;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CarteraRepository extends JpaRepository<Cartera, Long>, JpaSpecificationExecutor<Cartera> {

    boolean existsByVentaId(Long ventaId);

    Optional<Cartera> findByVentaId(Long ventaId);

    List<Cartera> findByClienteIdAndSaldoGreaterThanOrderByUltimaActualizacionAsc(Long clienteId, Double saldo);
}
