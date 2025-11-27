// Producto models
export interface ProductoResponse {
  id: number;
  nombre: string;
  descripcion: string;
  precioCompra: number; // BigDecimal in backend
  precioVenta: number;  // BigDecimal in backend
  stock: number;
  categoria: string;
  fechaRegistro: string; // ISO 8601
  estado:boolean;
}

export interface ProductoRequest {
  nombre: string;
  descripcion: string;
  precioCompra: number;
  precioVenta: number;
  stock: number;
  categoria: string;
}

export interface MovimientoEntradaProductoRequest {
  productoId: number;
  cantidad: number;
  observacion?: string | null;
}

export interface MovimientoEntradaMasivaRequest {
  movimientos: MovimientoEntradaProductoRequest[];
}
