export interface FiltroProductoPrecio {
  id: number;
  precioCompra: number;
  cantidad: number;
}

export interface FiltroProductoResponse {
  nombreProducto: string;
  cantidadPorCajas: number;
  precios: FiltroProductoPrecio[];
}

export interface FiltroClienteResponse {
  id: number;
  nombre: string;
}

export interface VentaFiltrosResponse {
  productos: FiltroProductoResponse[];
  clientes: FiltroClienteResponse[];
}
