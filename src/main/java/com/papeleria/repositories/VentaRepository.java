package com.papeleria.repositories;

import com.papeleria.models.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    List<Venta> findByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("select coalesce(sum(v.total), 0) from Venta v where v.fecha between :inicio and :fin")
    BigDecimal sumTotalByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("select coalesce(sum(dv.cantidad * (dv.producto.precioVenta - dv.producto.precioCompra)), 0) from DetalleVenta dv where dv.venta.fecha between :inicio and :fin")
    BigDecimal sumGananciaByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    Long countByFechaBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("select dv.producto.id, dv.producto.nombre, sum(dv.cantidad), sum(dv.subtotal) "
            + "from DetalleVenta dv group by dv.producto.id, dv.producto.nombre order by sum(dv.cantidad) desc")
    List<Object[]> findTopSellingProducts(Pageable pageable);

    @Query("select dv.producto.id, dv.producto.nombre, sum(dv.cantidad), sum(dv.subtotal) "
            + "from DetalleVenta dv where dv.venta.fecha between :inicio and :fin "
            + "group by dv.producto.id, dv.producto.nombre order by sum(dv.cantidad) desc")
    List<Object[]> findTopSellingProductsByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin, Pageable pageable);
}
