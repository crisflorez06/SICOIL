package com.SICOIL.mappers.venta;

import com.SICOIL.dtos.venta.DetalleVentaRequest;
import com.SICOIL.dtos.venta.DetalleVentaResponse;
import com.SICOIL.dtos.venta.VentaItemResponse;
import com.SICOIL.dtos.venta.VentaListadoResponse;
import com.SICOIL.dtos.venta.VentaRequest;
import com.SICOIL.dtos.venta.VentaResponse;
import com.SICOIL.models.Cliente;
import com.SICOIL.models.DetalleVenta;
import com.SICOIL.models.Producto;
import com.SICOIL.models.Usuario;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.ProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Component
public class VentaMapper {

    @Autowired
    private ProductoRepository productoRepository;

    public VentaListadoResponse toListado(Venta venta) {
        if (venta == null) {
            return null;
        }

        List<VentaItemResponse> items = venta.getDetalles() == null
                ? List.of()
                : venta.getDetalles().stream()
                .map(this::detalleToItem)
                .toList();

        return VentaListadoResponse.builder()
                .ventaId(venta.getId())
                .clienteNombre(venta.getCliente() != null ? venta.getCliente().getNombre() : null)
                .totalVenta(venta.getTotal())
                .tipoVenta(venta.getTipoVenta())
                .activa(venta.isActiva())
                .motivoAnulacion(venta.getMotivoAnulacion())
                .usuarioNombre(venta.getUsuario() != null ? venta.getUsuario().getUsuario() : null)
                .fechaRegistro(venta.getFechaRegistro())
                .items(items)
                .build();
    }

    public Venta requestToEntity(VentaRequest request, Usuario usuario, Cliente cliente) {

        Venta venta = new Venta();
        if (cliente == null) {
            throw new IllegalArgumentException("No se pudo determinar el cliente de la venta.");
        }

        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setTipoVenta(request.getTipoVenta());
        venta.setActiva(true);
        venta.setMotivoAnulacion(null);
        venta.setFechaRegistro(request.getFechaRegistro());

        double total = 0.0;
        List<DetalleVenta> detallesFinales = new ArrayList<>();

        // Por cada item del request, aplicar FIFO
        for (DetalleVentaRequest item : request.getItems()) {

            String nombre = item.getNombreProducto();

            // 1. Obtener todos los lotes de ese producto en orden FIFO
            List<Producto> lotes = productoRepository
                    .findByNombreIgnoreCaseOrderByFechaRegistroAsc(nombre);

            if (lotes.isEmpty()) {
                throw new IllegalArgumentException("No existe ningún lote para el producto: " + nombre);
            }

            int restante = item.getCantidad();

            // 2. Consumir lotes uno a uno
            for (Producto lote : lotes) {

                if (restante <= 0) break;

                int disponible = lote.getStock() != null ? lote.getStock() : 0;
                if (disponible <= 0) continue;

                int aDescontar = Math.min(restante, disponible);

                // Crear un detalle por lote consumido
                DetalleVenta det = new DetalleVenta();
                det.setProducto(lote);
                det.setCantidad(aDescontar);
                det.setSubtotal(aDescontar * item.getSubtotal());
                det.setVenta(venta);

                detallesFinales.add(det);
                total += det.getSubtotal();

                restante -= aDescontar;
            }

            if (restante > 0) {
                throw new IllegalArgumentException(
                        "Stock insuficiente para el producto '" + nombre +
                                "'. Faltan " + restante + " unidades."
                );
            }
        }

        venta.setDetalles(detallesFinales);
        venta.setTotal(total);

        return venta;
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

    private VentaItemResponse detalleToItem(DetalleVenta detalle) {
        Producto producto = detalle.getProducto();
        return VentaItemResponse.builder()
                .productoNombre(producto != null ? producto.getNombre() : null)
                .precioCompra(producto != null ? producto.getPrecioCompra() : null)
                .cantidad(detalle.getCantidad())
                .precioVenta(detalle.getSubtotal())
                .build();
    }

}
