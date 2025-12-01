package com.SICOIL.repositories;

import com.SICOIL.models.CarteraMovimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CarteraMovimientoRepository extends JpaRepository<CarteraMovimiento, Long>,
        JpaSpecificationExecutor<CarteraMovimiento> {
}
