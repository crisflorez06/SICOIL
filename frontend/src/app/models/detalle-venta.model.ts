export interface DetalleVentaRequest {
  nombreProducto: string;
  cantidad: number;
  subtotal: number;
}

export interface DetalleVentaResponse {
  producto: string;
  cantidad: number;
  subtotal: number;
}
