export interface ProductoRequest {
  nombre: string;
  cantidadPorCajas: number;
  precioCompra: number;
  stock: number;
}

export interface ProductoResponse {
  id: number;
  nombre: string;
  precioCompra: number;
  cantidadPorCajas: number;
  stock: number;
}

export interface ProductoActualizarRequest {
  nombre: string;
  cantidadPorCajas: number;
}

export interface ProductosDesagrupadosResponse {
  id: number;
  precioCompra: number;
  stock: number;
}

export interface ProductosAgrupadosResponse {
  nombre: string;
  stockTotal: number;
  cantidadPorCajas: number;
  variantes: ProductosDesagrupadosResponse[];
}

export interface PaginaProductoResponse {
  content: ProductosAgrupadosResponse[];
  page: number;
  size: number;
  totalPages: number;
  totalElements: number;
}

export interface IngresoProductoRequest {
  nombreProducto: string;
  precioCompra: number;
  cantidad: number;
}

export interface InventarioSalidaRequest {
  cantidad: number;
  observacion?: string | null;
}
