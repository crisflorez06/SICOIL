package com.SICOIL.repositories;

import com.SICOIL.models.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    List<Venta> findByFechaRegistroBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("select coalesce(sum(v.total), 0) from Venta v where v.fechaRegistro between :inicio and :fin")
    BigDecimal sumTotalByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select coalesce(sum(dv.subtotal - dv.producto.precioCompra), 0)
            from DetalleVenta dv
            where dv.venta.fechaRegistro between :inicio and :fin
            """)
    BigDecimal sumGananciaByFechaBetween(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    Long countByFechaRegistroBetween(LocalDateTime inicio, LocalDateTime fin);

    @Query("""
            select dv.producto.id, dv.producto.nombre, sum(dv.cantidad), sum(dv.subtotal)
            from DetalleVenta dv
            group by dv.producto.id, dv.producto.nombre
            order by sum(dv.cantidad) desc
            """)
    List<Object[]> findTopSellingProducts(Pageable pageable);

    @Query("""
            select dv.producto.id, dv.producto.nombre, sum(dv.cantidad), sum(dv.subtotal)
            from DetalleVenta dv
            where dv.venta.fechaRegistro between :inicio and :fin
            group by dv.producto.id, dv.producto.nombre
            order by sum(dv.cantidad) desc
            """)
    List<Object[]> findTopSellingProductsByFechaBetween(@Param("inicio") LocalDateTime inicio,
                                                        @Param("fin") LocalDateTime fin,
                                                        Pageable pageable);

    @Query("""
            select distinct v
            from Venta v
            left join fetch v.detalles d
            left join fetch v.cliente
            left join fetch v.usuario
            left join fetch d.producto
            """)
    List<Venta> findAllWithDetalleAndRelations();
}
