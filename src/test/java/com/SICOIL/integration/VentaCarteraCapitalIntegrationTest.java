package com.SICOIL.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.SICOIL.dtos.cartera.CarteraAbonoRequest;
import com.SICOIL.dtos.producto.IngresoProductoRequest;
import com.SICOIL.dtos.venta.DetalleVentaRequest;
import com.SICOIL.dtos.venta.VentaRequest;
import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.models.CapitalMovimiento;
import com.SICOIL.models.CapitalOrigen;
import com.SICOIL.models.Cartera;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.Producto;
import com.SICOIL.models.TipoVenta;
import com.SICOIL.models.Usuario;
import com.SICOIL.repositories.CapitalMovimientoRepository;
import com.SICOIL.repositories.CarteraRepository;
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
        DetalleVentaRequest item = new DetalleVentaRequest();
        item.setProductoId(productoId);
        item.setCantidad(cantidad);
        item.setSubtotal(precioUnitario * cantidad);

        VentaRequest request = new VentaRequest();
        request.setClienteId(clienteId);
        request.setTipoVenta(tipoVenta);
        request.setItems(List.of(item));
        return request;
    }
}
