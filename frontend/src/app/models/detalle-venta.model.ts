export interface DetalleVentaRequest {
  productoId: number;
  cantidad: number;
  subtotal: number;
}

export interface DetalleVentaResponse {
  producto: string;
  cantidad: number;
  subtotal: number;
}
