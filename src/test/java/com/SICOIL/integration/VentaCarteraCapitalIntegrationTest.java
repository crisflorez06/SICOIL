package com.SICOIL.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.SICOIL.dtos.cartera.CarteraAbonoDetalleResponse;
import com.SICOIL.dtos.cartera.CarteraAbonoRequest;
import com.SICOIL.dtos.producto.IngresoProductoRequest;
import com.SICOIL.dtos.venta.DetalleVentaRequest;
import com.SICOIL.dtos.venta.VentaRequest;
import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.models.CapitalMovimiento;
import com.SICOIL.models.CapitalOrigen;
import com.SICOIL.models.Cartera;
import com.SICOIL.models.CarteraMovimiento;
import com.SICOIL.models.CarteraMovimientoTipo;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.Producto;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.repositories.CapitalMovimientoRepository;
import com.SICOIL.repositories.CarteraRepository;
import com.SICOIL.repositories.CarteraMovimientoRepository;
import com.SICOIL.repositories.ClienteRepository;
import com.SICOIL.repositories.ProductoRepository;
import com.SICOIL.repositories.UsuarioRepository;
import com.SICOIL.services.InventarioService;
import com.SICOIL.services.cartera.CarteraService;
import com.SICOIL.services.security.UsuarioDetails;
import com.SICOIL.services.venta.VentaService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VentaCarteraCapitalIntegrationTest {

    @Autowired
    private VentaService ventaService;

    @Autowired
    private CarteraService carteraService;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CapitalMovimientoRepository capitalMovimientoRepository;

    @Autowired
    private CarteraRepository carteraRepository;

    @Autowired
    private CarteraMovimientoRepository carteraMovimientoRepository;

    @Autowired
    private InventarioService inventarioService;

    @BeforeEach
    void setUpSecurityContext() {
        SecurityContextHolder.clearContext();
        Usuario usuario = usuarioRepository.save(
                Usuario.builder()
                        .usuario("tester-" + UUID.randomUUID())
                        .contrasena("secret")
                        .build()
        );
        UsuarioDetails details = new UsuarioDetails(usuario);
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                details,
                usuario.getContrasena(),
                details.getAuthorities()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void crearVentaContado_actualizaInventarioYCapitalSinCartera() {
        Producto producto = crearProducto("Aceite Premium", 50d, 20);
        int stockInicial = producto.getStock();
        Cliente cliente = crearCliente("Cliente Contado");
        int cantidadVendida = 2;
        double precioUnitario = 150d;

        VentaRequest request = construirVentaRequest(
                cliente.getId(),
                producto.getId(),
                TipoVenta.CONTADO,
                cantidadVendida,
                precioUnitario
        );

        VentaResponse response = ventaService.crearVenta(request);

        assertThat(response.getTotal()).isEqualTo(precioUnitario * cantidadVendida);

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(actualizado.getStock()).isEqualTo(stockInicial - cantidadVendida);

        List<CapitalMovimiento> movimientos = capitalMovimientoRepository.findAll();
        assertThat(movimientos).hasSize(1);
        CapitalMovimiento movimiento = movimientos.get(0);
        assertThat(movimiento.getOrigen()).isEqualTo(CapitalOrigen.VENTA);
        assertThat(movimiento.getEsCredito()).isFalse();
        assertThat(movimiento.getMonto()).isEqualTo(precioUnitario * cantidadVendida);

        assertThat(carteraRepository.count()).isZero();
    }

    @Test
    void ventaCreditoYAbono_actualizaCarteraYCapital() {
        Producto producto = crearProducto("Aceite Credito", 80d, 15);
        Cliente cliente = crearCliente("Cliente Credito");
        int cantidad = 3;
        double precioUnitario = 200d;

        VentaRequest request = construirVentaRequest(
                cliente.getId(),
                producto.getId(),
                TipoVenta.CREDITO,
                cantidad,
                precioUnitario
        );

        VentaResponse response = ventaService.crearVenta(request);
        double totalVenta = precioUnitario * cantidad;

        Cartera cartera = carteraRepository.findByVentaId(response.getId()).orElseThrow();
        assertThat(cartera.getSaldo()).isEqualTo(totalVenta);

        List<CapitalMovimiento> despuesVenta = capitalMovimientoRepository.findAll();
        assertThat(despuesVenta).hasSize(1);
        CapitalMovimiento credito = despuesVenta.get(0);
        assertThat(credito.getEsCredito()).isTrue();
        assertThat(credito.getMonto()).isEqualTo(totalVenta);

        double abonoParcial = totalVenta / 2;
        String observacionAbono = "Pago parcial";
        CarteraAbonoRequest abonoRequest = new CarteraAbonoRequest();
        abonoRequest.setMonto(abonoParcial);
        abonoRequest.setObservacion(observacionAbono);

        carteraService.registrarAbono(cliente.getId(), abonoRequest);

        Cartera carteraActualizada = carteraRepository.findById(cartera.getId()).orElseThrow();
        assertThat(carteraActualizada.getSaldo()).isEqualTo(totalVenta - abonoParcial);

        List<CapitalMovimiento> movimientosFinales = capitalMovimientoRepository.findAll();
        assertThat(movimientosFinales).hasSize(2);
        CapitalMovimiento abonoMovimiento = movimientosFinales.stream()
                .filter(m -> !m.getEsCredito())
                .findFirst()
                .orElseThrow();
        assertThat(abonoMovimiento.getMonto()).isEqualTo(abonoParcial);
        assertThat(abonoMovimiento.getDescripcion()).contains(observacionAbono);
    }

    @Test
    void eliminarAbono_revierteSaldoYCreaMovimientoCapitalNegativo() {
        Producto producto = crearProducto("Aceite Reversion", 75d, 10);
        Cliente cliente = crearCliente("Cliente Reversion");
        double precioUnitario = 150d;
        int cantidad = 2;

        VentaResponse venta = ventaService.crearVenta(construirVentaRequest(
                cliente.getId(),
                producto.getId(),
                TipoVenta.CREDITO,
                cantidad,
                precioUnitario
        ));

        Cartera cartera = carteraRepository.findByVentaId(venta.getId()).orElseThrow();
        double totalVenta = precioUnitario * cantidad;
        assertThat(cartera.getSaldo()).isEqualTo(totalVenta);

        double abono = totalVenta / 2;
        CarteraAbonoRequest abonoRequest = new CarteraAbonoRequest();
        abonoRequest.setMonto(abono);
        abonoRequest.setObservacion("Pago parcial");
        carteraService.registrarAbono(cliente.getId(), abonoRequest);

        CarteraMovimiento movimientoAbono = carteraMovimientoRepository.findAll().stream()
                .filter(m -> m.getTipo() == CarteraMovimientoTipo.ABONO)
                .findFirst()
                .orElseThrow();

        CarteraAbonoRequest eliminarRequest = new CarteraAbonoRequest();
        eliminarRequest.setMonto(abono);
        eliminarRequest.setObservacion("Abono registrado por error");

        carteraService.eliminarAbono(cliente.getId(), movimientoAbono.getId(), eliminarRequest);

        Cartera carteraRestaurada = carteraRepository.findById(cartera.getId()).orElseThrow();
        assertThat(carteraRestaurada.getSaldo()).isEqualTo(totalVenta);
        assertThat(carteraMovimientoRepository.findById(movimientoAbono.getId())).isEmpty();

        List<CarteraAbonoDetalleResponse> abonos = carteraService.listarAbonos(cliente.getId(), null, null);
        assertThat(abonos).isNotEmpty();
        assertThat(abonos.get(0).getMonto()).isEqualTo(-abono);

        List<CarteraMovimiento> ajustes = carteraMovimientoRepository.findAll().stream()
                .filter(m -> m.getTipo() == CarteraMovimientoTipo.AJUSTE)
                .toList();
        assertThat(ajustes).hasSize(1);

        List<CapitalMovimiento> movimientosCapital = capitalMovimientoRepository.findByOrigenAndReferenciaId(
                CapitalOrigen.ABONO,
                venta.getId()
        );
        double totalLiquido = movimientosCapital.stream()
                .filter(m -> !m.getEsCredito())
                .mapToDouble(CapitalMovimiento::getMonto)
                .sum();
        assertThat(totalLiquido).isZero();
    }

    @Test
    void anularVentaContado_revierteStockYCapital() {
        Producto producto = crearProducto("Aceite Contado", 40d, 18);
        int stockInicial = producto.getStock();
        Cliente cliente = crearCliente("Cliente Anulacion Contado");

        VentaResponse venta = ventaService.crearVenta(construirVentaRequest(
                cliente.getId(),
                producto.getId(),
                TipoVenta.CONTADO,
                4,
                120d
        ));

        VentaResponse anulada = ventaService.anularVenta(venta.getId(), "Error en factura");
        assertThat(anulada.isActiva()).isFalse();

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(actualizado.getStock()).isEqualTo(stockInicial);

        List<CapitalMovimiento> movimientos = capitalMovimientoRepository.findByOrigenAndReferenciaId(
                CapitalOrigen.VENTA,
                venta.getId()
        );
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos).allMatch(m -> !m.getEsCredito());
        assertThat(movimientos.stream().mapToDouble(CapitalMovimiento::getMonto).sum()).isEqualTo(0d);
    }

    @Test
    void anularVentaCredito_eliminaCreditoYPendientes() {
        Producto producto = crearProducto("Aceite Credito Anulacion", 60d, 25);
        Cliente cliente = crearCliente("Cliente Anulacion Credito");
        int cantidad = 5;
        double precio = 180d;

        VentaResponse venta = ventaService.crearVenta(construirVentaRequest(
                cliente.getId(),
                producto.getId(),
                TipoVenta.CREDITO,
                cantidad,
                precio
        ));

        ventaService.anularVenta(venta.getId(), "Cliente rechazo");

        Cartera cartera = carteraRepository.findByVentaId(venta.getId()).orElseThrow();
        assertThat(cartera.getSaldo()).isZero();

        List<CapitalMovimiento> movimientos = capitalMovimientoRepository.findByOrigenAndReferenciaId(
                CapitalOrigen.VENTA,
                venta.getId()
        );
        assertThat(movimientos).hasSize(2);
        assertThat(movimientos).allMatch(CapitalMovimiento::getEsCredito);
        assertThat(movimientos.stream().mapToDouble(CapitalMovimiento::getMonto).sum()).isEqualTo(0d);
    }

    @Test
    void registrarIngresoProducto_incrementaStockYRegistraCompra() {
        Producto producto = crearProducto("Aceite Compra", 70d, 12);
        int stockInicial = producto.getStock();
        int cantidad = 6;

        IngresoProductoRequest request = new IngresoProductoRequest();
        request.setNombreProducto(producto.getNombre());
        request.setPrecioCompra(producto.getPrecioCompra());
        request.setCantidad(cantidad);

        inventarioService.registrarIngresoProducto(request);

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(actualizado.getStock()).isEqualTo(stockInicial + cantidad);

        List<CapitalMovimiento> movimientos = capitalMovimientoRepository.findByOrigenAndReferenciaId(
                CapitalOrigen.COMPRA,
                producto.getId()
        );
        assertThat(movimientos).hasSize(1);
        CapitalMovimiento compra = movimientos.get(0);
        assertThat(compra.getMonto()).isEqualTo(-(producto.getPrecioCompra() * cantidad));
    }

    @Test
    void registrarDevolucion_noGeneraMovimientosDeCapital() {
        Producto producto = crearProducto("Aceite Devuelto", 35d, 10);
        int stockInicial = producto.getStock();
        int cantidadDevuelta = 4;
        long movimientosAntes = capitalMovimientoRepository.count();

        inventarioService.registrarDevolucion(producto.getId(), cantidadDevuelta, "Devolución simple");

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(actualizado.getStock()).isEqualTo(stockInicial + cantidadDevuelta);
        assertThat(capitalMovimientoRepository.count()).isEqualTo(movimientosAntes);
    }

    @Test
    void registrarIngresoProducto_conNuevoPrecioCreaVariante() {
        Producto base = crearProducto("Aceite Variantes", 55d, 8);
        int stockBase = base.getStock();

        IngresoProductoRequest request = new IngresoProductoRequest();
        request.setNombreProducto(base.getNombre());
        request.setPrecioCompra(base.getPrecioCompra() + 10);
        request.setCantidad(5);

        Producto variante = inventarioService.registrarIngresoProducto(request);

        assertThat(variante.getId()).isNotEqualTo(base.getId());
        assertThat(variante.getNombre()).isEqualTo(base.getNombre());
        assertThat(variante.getPrecioCompra()).isEqualTo(request.getPrecioCompra());
        assertThat(variante.getStock()).isEqualTo(request.getCantidad());

        Producto baseActualizado = productoRepository.findById(base.getId()).orElseThrow();
        assertThat(baseActualizado.getStock()).isEqualTo(stockBase);

        List<Producto> variantes = productoRepository.findAll().stream()
                .filter(p -> p.getNombre().equals(base.getNombre()))
                .toList();
        assertThat(variantes).hasSize(2);

        List<CapitalMovimiento> movimientos = capitalMovimientoRepository.findByOrigenAndReferenciaId(
                CapitalOrigen.COMPRA,
                variante.getId()
        );
        assertThat(movimientos).hasSize(1);
        assertThat(movimientos.get(0).getMonto()).isEqualTo(-(request.getPrecioCompra() * request.getCantidad()));
    }

    @Test
    void crearVentaConMultiplesProductos_descuentaStocksYRegistraCapital() {
        Producto productoA = crearProducto("Aceite Familiar", 60d, 30);
        Producto productoB = crearProducto("Aceite Individual", 45d, 25);
        int stockInicialA = productoA.getStock();
        int stockInicialB = productoB.getStock();
        Cliente cliente = crearCliente("Cliente Multiproducto");

        DetalleVentaRequest itemA = construirDetalleVentaRequest(productoA.getId(), 3, 120d);
        DetalleVentaRequest itemB = construirDetalleVentaRequest(productoB.getId(), 4, 90d);
        VentaRequest request = construirVentaRequest(
                cliente.getId(),
                TipoVenta.CONTADO,
                List.of(itemA, itemB)
        );

        VentaResponse response = ventaService.crearVenta(request);
        assertThat(response.getDetalles()).hasSize(2);
        double totalEsperado = (120d * 3) + (90d * 4);
        assertThat(response.getTotal()).isEqualTo(totalEsperado);

        Producto actualizadoA = productoRepository.findById(productoA.getId()).orElseThrow();
        Producto actualizadoB = productoRepository.findById(productoB.getId()).orElseThrow();
        assertThat(actualizadoA.getStock()).isEqualTo(stockInicialA - 3);
        assertThat(actualizadoB.getStock()).isEqualTo(stockInicialB - 4);

        List<CapitalMovimiento> movimientos = capitalMovimientoRepository.findByOrigenAndReferenciaId(
                CapitalOrigen.VENTA,
                response.getId()
        );
        assertThat(movimientos).hasSize(1);
        CapitalMovimiento movimiento = movimientos.get(0);
        assertThat(movimiento.getMonto()).isEqualTo(totalEsperado);
        assertThat(movimiento.getEsCredito()).isFalse();
    }

    @Test
    void crearVentaConStockInsuficiente_fallaSinAfectarInventarioNiCapital() {
        Producto producto = crearProducto("Aceite Limitado", 65d, 2);
        Cliente cliente = crearCliente("Cliente Sin Stock");
        int cantidadSolicitada = producto.getStock() + 3;
        VentaRequest request = construirVentaRequest(
                cliente.getId(),
                producto.getId(),
                TipoVenta.CONTADO,
                cantidadSolicitada,
                140d
        );
        long movimientosAntes = capitalMovimientoRepository.count();

        assertThatThrownBy(() -> ventaService.crearVenta(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stock disponible");

        Producto actualizado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(actualizado.getStock()).isEqualTo(producto.getStock());
        assertThat(capitalMovimientoRepository.count()).isEqualTo(movimientosAntes);
    }

    private Producto crearProducto(String nombre, double precioCompra, int stock) {
        Producto producto = Producto.builder()
                .nombre(nombre)
                .precioCompra(precioCompra)
                .cantidadPorCajas(1)
                .stock(stock)
                .build();
        return productoRepository.save(producto);
    }

    private Cliente crearCliente(String nombre) {
        Cliente cliente = Cliente.builder()
                .nombre(nombre)
                .telefono("5551234")
                .direccion("Calle 1")
                .build();
        return clienteRepository.save(cliente);
    }

    private VentaRequest construirVentaRequest(Long clienteId,
                                               Long productoId,
                                               TipoVenta tipoVenta,
                                               int cantidad,
                                               double precioUnitario) {
        DetalleVentaRequest item = construirDetalleVentaRequest(productoId, cantidad, precioUnitario);
        return construirVentaRequest(clienteId, tipoVenta, List.of(item));
    }

    private VentaRequest construirVentaRequest(Long clienteId,
                                               TipoVenta tipoVenta,
                                               List<DetalleVentaRequest> items) {
        VentaRequest request = new VentaRequest();
        request.setClienteId(clienteId);
        request.setTipoVenta(tipoVenta);
        request.setItems(items);
        return request;
    }

    private DetalleVentaRequest construirDetalleVentaRequest(Long productoId,
                                                             int cantidad,
                                                             double precioUnitario) {
        DetalleVentaRequest item = new DetalleVentaRequest();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        item.setSubtotal(precioUnitario * cantidad);
        return item;
    }
}
