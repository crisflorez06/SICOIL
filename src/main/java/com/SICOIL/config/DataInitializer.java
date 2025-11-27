package com.SICOIL.config;

import com.SICOIL.models.DetalleVenta;
import com.SICOIL.models.Movimiento;
import com.SICOIL.models.MovimientoTipo;
import com.SICOIL.models.Producto;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.MovimientoRepository;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.repositories.VentaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private MovimientoRepository movimientoRepository;

    @Override
    public void run(String... args) throws Exception {
        if (productoRepository.count() == 0) {
            System.out.println("No hay productos en la base de datos. Creando datos de ejemplo con moneda colombiana...");

            Producto p1 = Producto.builder()
                    .nombre("Lápiz HB")
                    .descripcion("Lápiz de grafito para escritura y dibujo")
                    .precioCompra(new BigDecimal("500"))
                    .precioVenta(new BigDecimal("1000"))
                    .stock(100)
                    .categoria("Escritura")
                    .build();

            Producto p2 = Producto.builder()
                    .nombre("Cuaderno A4")
                    .descripcion("Cuaderno de 100 hojas cuadriculadas")
                    .precioCompra(new BigDecimal("2500"))
                    .precioVenta(new BigDecimal("4000"))
                    .stock(50)
                    .categoria("SICOIL")
                    .build();

            Producto p3 = Producto.builder()
                    .nombre("Goma de borrar")
                    .descripcion("Goma de borrar blanca para lápiz")
                    .precioCompra(new BigDecimal("300"))
                    .precioVenta(new BigDecimal("800"))
                    .stock(200)
                    .categoria("Accesorios")
                    .build();

            productoRepository.saveAll(List.of(p1, p2, p3));

            System.out.println("Datos de ejemplo creados.");
        } else {
            System.out.println("La base de datos ya contiene datos de productos. No se crearán datos de ejemplo.");
        }

        if (ventaRepository.count() == 0) {
            System.out.println("No hay ventas en la base de datos. Creando datos de ejemplo...");

            List<Producto> productos = productoRepository.findAll();

            Venta v1 = Venta.builder()
                    .fecha(LocalDateTime.now().minusDays(1))
                    .metodoPago("Efectivo")
                    .total(new BigDecimal("0"))
                    .build();

            DetalleVenta dv1 = DetalleVenta.builder()
                    .producto(productos.get(0))
                    .cantidad(2)
                    .precioUnitario(productos.get(0).getPrecioVenta())
                    .subtotal(productos.get(0).getPrecioVenta().multiply(new BigDecimal("2")))
                    .build();
            v1.agregarDetalle(dv1);
            v1.setTotal(v1.getTotal().add(dv1.getSubtotal()));

            DetalleVenta dv2 = DetalleVenta.builder()
                    .producto(productos.get(1))
                    .cantidad(1)
                    .precioUnitario(productos.get(1).getPrecioVenta())
                    .subtotal(productos.get(1).getPrecioVenta())
                    .build();
            v1.agregarDetalle(dv2);
            v1.setTotal(v1.getTotal().add(dv2.getSubtotal()));

            ventaRepository.save(v1);

            System.out.println("Datos de ventas de ejemplo creados.");
        } else {
            System.out.println("La base de datos ya contiene datos de ventas. No se crearán datos de ejemplo.");
        }

        if (movimientoRepository.count() == 0) {
            System.out.println("No hay movimientos en la base de datos. Creando datos de ejemplo...");

            List<Producto> productos = productoRepository.findAll();

            if (productos.isEmpty()) {
                System.out.println("No se pudieron crear movimientos porque no hay productos disponibles.");
                return;
            }

            LocalDate hoy = LocalDate.now();

            Movimiento m1 = Movimiento.builder()
                    .producto(productos.get(0))
                    .cantidad(25)
                    .tipo(MovimientoTipo.INGRESO)
                    .observacion("Reposición inicial de inventario.")
                    .fechaMovimiento(hoy.minusDays(3).atTime(LocalTime.of(10, 30)))
                    .build();

            Movimiento m2 = Movimiento.builder()
                    .producto(productos.size() > 1 ? productos.get(1) : productos.get(0))
                    .cantidad(15)
                    .tipo(MovimientoTipo.INGRESO)
                    .observacion("Ingreso desde proveedor principal.")
                    .fechaMovimiento(hoy.minusDays(2).atTime(LocalTime.of(15, 45)))
                    .build();

            Movimiento m3 = Movimiento.builder()
                    .producto(productos.size() > 2 ? productos.get(2) : productos.get(0))
                    .cantidad(30)
                    .tipo(MovimientoTipo.INGRESO)
                    .observacion("Reposición por temporada escolar.")
                    .fechaMovimiento(hoy.minusDays(1).atTime(LocalTime.of(9, 15)))
                    .build();

            movimientoRepository.saveAll(List.of(m1, m2, m3));

            System.out.println("Datos de movimientos de ejemplo creados.");
        } else {
            System.out.println("La base de datos ya contiene datos de movimientos. No se crearán datos de ejemplo.");
        }
    }
}
