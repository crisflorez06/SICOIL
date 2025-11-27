package com.papeleria.repositories;

import com.papeleria.models.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long>, JpaSpecificationExecutor<Movimiento> {
}
