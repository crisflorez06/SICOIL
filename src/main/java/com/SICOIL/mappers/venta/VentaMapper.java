package com.SICOIL.mappers.venta;

import com.SICOIL.dtos.venta.DetalleVentaRequest;
import com.SICOIL.dtos.venta.DetalleVentaResponse;
import com.SICOIL.dtos.venta.VentaDetalleTablaResponse;
import com.SICOIL.dtos.venta.VentaRequest;
import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.DetalleVenta;
import com.SICOIL.models.Producto;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class VentaMapper {

    public VentaDetalleTablaResponse toResponse(DetalleVenta detalle) {
        if (detalle == null) {
            return null;
        }

        Producto producto = detalle.getProducto();
        Venta venta = detalle.getVenta();
        Usuario usuario = venta != null ? venta.getUsuario() : null;

        return VentaDetalleTablaResponse.builder()
                .detalleId(detalle.getId())
                .productoNombre(producto != null ? producto.getNombre() : null)
                .precioCompra(producto != null ? producto.getPrecioCompra() : null)
                .cantidad(detalle.getCantidad())
                .subtotal(detalle.getSubtotal())
                .tipoVenta(venta != null ? venta.getTipoVenta() : null)
                .activa(venta != null && venta.isActiva())
                .motivoAnulacion(venta != null ? venta.getMotivoAnulacion() : null)
                .clienteNombre(venta != null && venta.getCliente() != null ? venta.getCliente().getNombre() : null)
                .usuarioNombre(usuario != null ? usuario.getUsuario() : null)
                .fechaRegistro(venta != null ? venta.getFechaRegistro() : null)
                .build();
    }

    public Venta requestToEntity(VentaRequest request, Usuario usuario, Cliente cliente, Function<Long, Producto> loadProducto){

        Venta venta = new Venta();
        if (cliente == null) {
            throw new IllegalArgumentException("No se pudo determinar el cliente de la venta.");
        }
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setTipoVenta(request.getTipoVenta());
        venta.setActiva(true);
        venta.setMotivoAnulacion(null);
        double total = 0.0;

        List<DetalleVenta> detalles = new ArrayList<>();

        for (DetalleVentaRequest item : request.getItems()) {

            // Cargar el producto
            Producto producto = loadProducto.apply(item.getProductoId());

            DetalleVenta detalle = detalleRequestToEntity(item, producto);
            detalle.setVenta(venta);
            total += detalle.getSubtotal();
            detalles.add(detalle);
        }
        venta.setDetalles(detalles);
        venta.setTotal(total);
        return venta;

    }

    private DetalleVenta detalleRequestToEntity(DetalleVentaRequest request, Producto producto){

        DetalleVenta detalleVenta = new DetalleVenta();


        detalleVenta.setProducto(producto);
        detalleVenta.setCantidad(request.getCantidad());
        detalleVenta.setSubtotal(request.getSubtotal());

        return detalleVenta;
    }

    public VentaResponse entityToResponse(Venta venta) {
        if (venta == null) {
            return null;
        }

        VentaResponse response = new VentaResponse();
        response.setId(venta.getId());
        if (venta.getCliente() != null) {
            response.setClienteId(venta.getCliente().getId());
            response.setClienteNombre(venta.getCliente().getNombre());
        }
        if (venta.getUsuario() != null) {
            response.setUsuarioId(venta.getUsuario().getId());
            response.setUsuarioNombre(venta.getUsuario().getUsuario());
        }
        response.setTipoVenta(venta.getTipoVenta());
        response.setActiva(venta.isActiva());
        response.setMotivoAnulacion(venta.getMotivoAnulacion());
        response.setTotal(venta.getTotal());
        response.setFechaRegistro(venta.getFechaRegistro());

        List<DetalleVentaResponse> detalleResponses = venta.getDetalles()
                .stream()
                .map(this::detalleEntityToResponse)
                .toList();
        response.setDetalles(detalleResponses);

        return response;
    }

    private DetalleVentaResponse detalleEntityToResponse(DetalleVenta detalle) {
        DetalleVentaResponse response = new DetalleVentaResponse();
        if (detalle.getProducto() != null) {
            response.setProducto(detalle.getProducto().getNombre());
        }
        response.setCantidad(detalle.getCantidad());
        response.setSubtotal(detalle.getSubtotal());
        return response;
    }

}
