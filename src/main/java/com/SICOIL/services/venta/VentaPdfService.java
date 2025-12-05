package com.SICOIL.services.venta;

import com.SICOIL.models.DetalleVenta;
import com.SICOIL.models.Venta;
import com.SICOIL.repositories.VentaRepository;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import jakarta.persistence.EntityNotFoundException;
import java.io.ByteArrayOutputStream;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class VentaPdfService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final NumberFormat MONEY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    private final VentaRepository ventaRepository;

    public byte[] generarComprobante(Long ventaId) {
        Venta venta = ventaRepository.findByIdWithDetalleAndRelations(ventaId)
                .orElseThrow(() -> new EntityNotFoundException("Venta no encontrada con ID: " + ventaId));

        log.info("Generando comprobante PDF para la venta {}", ventaId);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument, PageSize.A4);
            document.setMargins(36, 36, 48, 36);

            agregarEncabezado(document, venta);
            agregarDatosPrincipales(document, venta);
            agregarTablaProductos(document, venta);
            agregarResumen(document, venta);
            agregarSeccionFirma(document);

            document.close();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            log.error("Error generando comprobante para venta {}", ventaId, ex);
            throw new IllegalStateException("No se pudo generar el comprobante de la venta " + ventaId, ex);
        }
    }

    private void agregarEncabezado(Document document, Venta venta) {
        String fechaRegistro = venta.getFechaRegistro() != null
                ? DATE_FORMATTER.format(venta.getFechaRegistro())
                : DATE_FORMATTER.format(LocalDateTime.now());

        document.add(new Paragraph("SICOIL")
                .setBold()
                .setFontSize(18)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Comprobante de Venta")
                .setFontSize(14)
                .setTextAlignment(TextAlignment.CENTER));
        document.add(new Paragraph("Generado el: " + fechaRegistro)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20));
    }

    private void agregarDatosPrincipales(Document document, Venta venta) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        agregarFilaClaveValor(table, "Venta #", venta.getId().toString());
        agregarFilaClaveValor(table, "Cliente", venta.getCliente() != null ? venta.getCliente().getNombre() : "N/D");
        String tipoVenta = venta.getTipoVenta() != null ? venta.getTipoVenta().name() : "N/D";
        String totalFormat = venta.getTotal() != null ? MONEY_FORMAT.format(venta.getTotal()) : MONEY_FORMAT.format(0);

        agregarFilaClaveValor(table, "Tipo de venta", tipoVenta);
        agregarFilaClaveValor(table, "Total", totalFormat);
        agregarFilaClaveValor(table, "Registrado por", venta.getUsuario() != null ? venta.getUsuario().getUsuario() : "N/D");
        agregarFilaClaveValor(table, "Estado", venta.isActiva() ? "Activa" : "Anulada");

        if (venta.getCliente() != null) {
            agregarFilaClaveValor(table, "Teléfono cliente",
                    venta.getCliente().getTelefono() != null ? venta.getCliente().getTelefono() : "-");
            agregarFilaClaveValor(table, "Dirección cliente",
                    venta.getCliente().getDireccion() != null ? venta.getCliente().getDireccion() : "-");
        }

        document.add(table.setMarginBottom(15));
    }

    private void agregarTablaProductos(Document document, Venta venta) {
        document.add(new Paragraph("Detalle de productos").setBold().setMarginBottom(5));

        List<DetalleVenta> detalles = venta.getDetalles() != null ? venta.getDetalles() : Collections.emptyList();

        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 1, 2}))
                .useAllAvailableWidth();

        table.addHeaderCell(crearCeldaEncabezado("Producto"));
        table.addHeaderCell(crearCeldaEncabezado("Cantidad"));
        table.addHeaderCell(crearCeldaEncabezado("Subtotal"));

        for (DetalleVenta detalle : detalles) {
            String producto = detalle.getProducto() != null ? detalle.getProducto().getNombre() : "Producto N/D";
            table.addCell(crearCeldaDato(producto));
            table.addCell(crearCeldaDato(detalle.getCantidad() != null ? detalle.getCantidad().toString() : "-")
                    .setTextAlignment(TextAlignment.CENTER));
            table.addCell(crearCeldaDato(detalle.getSubtotal() != null ? MONEY_FORMAT.format(detalle.getSubtotal()) : "-")
                    .setTextAlignment(TextAlignment.RIGHT));
        }

        if (detalles.isEmpty()) {
            table.addCell(new Cell(1, 3)
                    .add(new Paragraph("La venta no tiene detalles registrados"))
                    .setTextAlignment(TextAlignment.CENTER));
        }

        document.add(table.setMarginBottom(15));
    }

    private void agregarResumen(Document document, Venta venta) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();

        int cantidadItems = venta.getDetalles() != null ? venta.getDetalles().size() : 0;
        agregarFilaClaveValor(table, "Total de ítems", String.valueOf(cantidadItems));
        String totalVenta = venta.getTotal() != null ? MONEY_FORMAT.format(venta.getTotal()) : MONEY_FORMAT.format(0);
        agregarFilaClaveValor(table, "Total venta", totalVenta);

        if (!venta.isActiva() && venta.getMotivoAnulacion() != null) {
            agregarFilaClaveValor(table, "Motivo anulación", venta.getMotivoAnulacion());
        }

        document.add(table.setMarginBottom(25));
    }

    private void agregarSeccionFirma(Document document) {
        document.add(new Paragraph("Recibí conforme:")
                .setBold()
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth();

        table.addCell(new Cell()
                .add(new Paragraph(" "))
                .setHeight(40)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1)));
        table.addCell(new Cell()
                .add(new Paragraph("Firma del cliente"))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorder(Border.NO_BORDER));

        document.add(table);
    }

    private void agregarFilaClaveValor(Table table, String clave, String valor) {
        table.addCell(crearCeldaClave(clave));
        table.addCell(crearCeldaDato(valor));
    }

    private Cell crearCeldaClave(String texto) {
        return new Cell()
                .add(new Paragraph(texto).setBold())
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
    }

    private Cell crearCeldaDato(String texto) {
        return new Cell()
                .add(new Paragraph(texto))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.LEFT);
    }

    private Cell crearCeldaEncabezado(String texto) {
        return new Cell()
                .add(new Paragraph(texto).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
    }
}
