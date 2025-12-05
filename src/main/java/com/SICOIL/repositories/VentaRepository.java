package com.SICOIL.repositories;

import com.SICOIL.models.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
            where (:inicio is null or dv.venta.fechaRegistro >= :inicio)
              and (:fin is null or dv.venta.fechaRegistro <= :fin)
            group by dv.producto.id, dv.producto.nombre
            order by sum(dv.cantidad) desc
            """)
    List<Object[]> findTopSellingProducts(@Param("inicio") LocalDateTime inicio,
                                          @Param("fin") LocalDateTime fin,
                                          Pageable pageable);

    @Query("""
            select coalesce(sum(dv.cantidad), 0)
            from DetalleVenta dv
            where (:inicio is null or dv.venta.fechaRegistro >= :inicio)
              and (:fin is null or dv.venta.fechaRegistro <= :fin)
            """)
    Long sumCantidadVendida(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select coalesce(sum(
                case
                    when dv.producto.cantidadPorCajas > 0 then dv.cantidad * 1.0 / dv.producto.cantidadPorCajas
                    else 0
                end
            ), 0)
            from DetalleVenta dv
            where (:inicio is null or dv.venta.fechaRegistro >= :inicio)
              and (:fin is null or dv.venta.fechaRegistro <= :fin)
            """)
    Double sumCajasVendidas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select coalesce(sum(v.total), 0)
            from Venta v
            where (:inicio is null or v.fechaRegistro >= :inicio)
              and (:fin is null or v.fechaRegistro <= :fin)
            """)
    Double sumTotalVentas(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    @Query("""
            select v.cliente.id, v.cliente.nombre, count(v), sum(v.total)
            from Venta v
            where (:inicio is null or v.fechaRegistro >= :inicio)
              and (:fin is null or v.fechaRegistro <= :fin)
            group by v.cliente.id, v.cliente.nombre
            order by sum(v.total) desc
            """)
    List<Object[]> findTopClients(@Param("inicio") LocalDateTime inicio,
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

    @Query("""
            select v
            from Venta v
            left join fetch v.detalles d
            left join fetch v.cliente
            left join fetch v.usuario
            left join fetch d.producto
            where v.id = :ventaId
            """)
    Optional<Venta> findByIdWithDetalleAndRelations(@Param("ventaId") Long ventaId);
}
