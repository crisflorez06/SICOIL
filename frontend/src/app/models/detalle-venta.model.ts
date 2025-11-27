// src/app/models/detalle-venta.model.ts
export interface DetalleVentaRequest {
  productoId: number;
  cantidad: number;
}

export interface DetalleVentaResponse {
  id: number;
  productoId: number;
  productoNombre: string;
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
  fecha:string;
}
