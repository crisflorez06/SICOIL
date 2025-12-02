# Análisis funcional del backend SICOIL

Este documento resume las entidades centrales (modelos JPA) y los servicios disponibles en la aplicación Spring Boot ubicada en `src/main/java/com/SICOIL`. El objetivo es facilitar la revisión funcional del flujo completo de cartera, capital, ventas, inventario y seguridad.

---

## Modelos de dominio

Todos los modelos se ubican en `src/main/java/com/SICOIL/models`.

### CapitalMovimiento
- Tabla `capital_movimientos`.
- Campos: `id`, `origen` (`CapitalOrigen`), `referenciaId` (referencia externa opcional), `monto` (positivo para entradas o créditos, negativo para salidas), `esCredito` (diferencia operaciones a crédito de las de contado), `descripcion`, `creadoEn` (asignado con `@PrePersist`) y `usuario` (relación `ManyToOne` con `Usuario`).
- Se utiliza para auditar impacto en capital por ventas, compras e inyecciones.

### CapitalOrigen (enum)
- Valores: `VENTA`, `COMPRA`, `INYECCION`. Conduce la lógica de `CapitalService`.

### Cartera
- Tabla `cartera`.
- Campos: `id`, `cliente` (`ManyToOne`), `venta` (`ManyToOne`), `saldo`, `ultimaActualizacion` (se refresca en `@PrePersist` y `@PreUpdate`).
- Representa la deuda viva generada por ventas a crédito.

### CarteraMovimiento
- Tabla `cartera_movimientos`.
- Campos: `id`, `cartera`, `tipo` (`CarteraMovimientoTipo`), `monto`, `usuario`, `observacion`, `fecha` (sellado en `@PrePersist`).
- Audita créditos, abonos y ajustes aplicados a cada `Cartera`.

### CarteraMovimientoTipo (enum)
- Valores: `CREDITO`, `ABONO`, `AJUSTE`.

### Cliente
- Tabla `clientes`.
- Campos: `id`, `nombre`, `telefono`, `direccion`, `fechaRegistro` (`@PrePersist`).
- No se almacenan correos; el nombre es obligatorio y único por validación de servicio/repositorio.

### Compra y CompraDetalle
- `Compra`: tabla `compras` con `id`, `usuario`, `total`, `fechaRegistro`, `detalles` (`List<CompraDetalle>` con cascada).
- `CompraDetalle`: tabla `compra_detalle` con `id`, `compra`, `producto`, `cantidad`, `costoCaja`, `subtotal`.
- Se usa para entradas de inventario asociadas a compras; expone método `agregarDetalle` que sincroniza la relación bidireccional.

### DetalleVenta
- Tabla `venta_detalle`.
- Campos: `id`, `cantidad`, `subtotal`, `producto`, `venta`.
- Posee referencias `@JsonBackReference` para evitar ciclos en serialización.

### Kardex
- Tabla `kardex`.
- Campos: `id`, `producto`, `usuario`, `cantidad`, `tipo` (`MovimientoTipo`), `comentario`, `fechaRegistro` (`@PrePersist`).
- Lleva historial de entradas/salidas de inventario.

### MovimientoTipo (enum)
- Valores: `ENTRADA`, `SALIDA`.

### Producto
- Tabla `productos`.
- Campos: `id`, `nombre`, `precioCompra`, `cantidadPorCajas`, `stock`.
- Incluye constructor adicional para clonar un producto base cuando cambia el precio de compra.

### TipoVenta (enum)
- Valores: `CONTADO`, `CREDITO`.

### Usuario
- Tabla `usuarios`.
- Campos: `id`, `usuario` (único), `contrasena`.
- Se utiliza tanto para autenticación como para auditar movimientos.

### Venta
- Tabla `ventas`.
- Campos: `id`, `cliente`, `usuario`, `tipoVenta`, `activa` (true por defecto), `motivoAnulacion`, `total`, `fechaRegistro`, `detalles`.
- `@PrePersist` asegura `fechaRegistro`.
- El campo `activa` controla anulaciones y se utiliza en los filtros del servicio.

---

## Servicios y flujo de métodos

### CapitalService (`services/capital/CapitalService.java`)
Responsable de registrar movimientos financieros derivados de inventario, ventas y abonos. Todas las operaciones validan entradas y llaman a `capitalMovimientoRepository`.

- `registrarIngresoInventario(producto, costoUnitario, cantidad, referencia)`: valida producto/costos, calcula total negativo (egreso) y guarda movimiento `COMPRA` para reflejar salida de capital en reabastecimiento.
- `registrarVentaContado(venta)`: verifica `TipoVenta.CONTADO`, registra entrada con monto positivo y `esCredito=false`.
- `registrarVentaCredito(venta)`: registra total como crédito (`esCredito=true`) dejando pendiente hasta abono.
- `registrarAbonoCartera(cartera, monto, descripcion)`: genera entrada por pagos de cartera; se apoya en `obtenerUsuarioMovimiento`.
- `revertirVenta(venta)`: invierte el efecto capital cuando una venta se anula; para contado registra salida real, para crédito descuenta el crédito pendiente.
- `registrarInyeccionCapital(monto, descripcion)`: crea entradas de capital con origen `INYECCION`.
- `obtenerMovimientos(filtro, pageable)`: aplica `CapitalMovimientoSpecification` para búsquedas por origen, crédito, referencia, descripción y rango de fechas.
- `obtenerResumen(desde, hasta)`: consolida saldo real, entradas, salidas, créditos pendientes y ganancias (entradas reales menos salidas reales) cruzando `capitalMovimientoRepository` y `carteraRepository`.
- Métodos privados relevantes: `registrarMovimiento` (centraliza persistencia y logging), `obtenerUsuarioMovimiento` (toma usuario actual con fallback nulo), `validarVenta` y `defaultValue`; el mapeo a DTO se delega al `CapitalMovimientoMapper`.

### CarteraService (`services/cartera/CarteraService.java`)
Gestiona la vida de una cuenta por cobrar asociada a ventas a crédito y sincroniza movimientos de capital.

- `registrarVentaEnCartera(venta)`: solo para `TipoVenta.CREDITO`; crea `Cartera` con saldo igual al total de la venta y genera un movimiento `CREDITO`.
- `ajustarPorAnulacion(venta, usuario, observacion)`: al anular una venta reduce saldo a cero y registra `AJUSTE`.
- `listarAbonos(clienteId, desde, hasta)` / `listarCreditos(...)`: consultan movimientos por tipo usando especificaciones y devuelven DTOs (mapa con `CarteraMovimientoMapper`).
- `registrarAbono(clienteId, request)`: valida monto y distribución. Se recorre cada `Cartera` pendiente (ordenada por `ultimaActualizacion`), se aplica la porción correspondiente, se registra movimiento `ABONO`, se actualiza `saldo` y se invoca `capitalService.registrarAbonoCartera`. Devuelve lista de abonos aplicados.
- `listarPendientes(nombreCliente, desde, hasta)`: filtra carteras con saldo > 0, agrupa por cliente y arma un resumen (`CarteraResumenResponse`) incluyendo totales de créditos y abonos en el rango dado.
- Funciones auxiliares: `agruparPorCliente`, `obtenerMovimientos` (aplica `CarteraMovimientoSpecification`), `construirResumenItem`, `construirObservacionAbono`, `calcularTotalesPorCliente`, `registrarMovimiento` (valida montos) y `TotalesMovimiento` (contador interno).

### ClienteService (`services/cliente/ClienteService.java`)
CRUD ligero sobre `Cliente`.

- `buscarPorId(id)`: obtiene cliente o lanza `EntityNotFoundException`.
- `traerTodos(nombreFiltro)`: usa `ClienteSpecification` para coincidencia parcial y mapea a DTO.
- `crearCliente(request)`: valida campos obligatorios, asegura unicidad (por nombre), transforma con `ClienteMapper` y persiste.

### InventarioService (`services/InventarioService.java`)
Encapsula operaciones sobre stock y las sincroniza con Kardex y Capital.

- `registrarMovimiento(productoId, cantidad, observacion)`: aumenta stock (entrada manual o por anulación), registra movimiento de kardex entrada y, si hay precio de compra definido, genera movimiento de capital (egreso).
- `registrarSalida(productoId, cantidad, observacion)`: valida stock suficiente, descuenta y registra SALIDA en kardex.
- `registrarIngresoProducto(request)`: soporta múltiples precios por producto.
  - Busca coincidencias por nombre y precio. Si existe, suma al stock existente, registra ENTRADA en kardex y egreso en capital.
  - Si cambia el precio, clona producto base creando una nueva variante con stock igual a la cantidad recibida, registrando movimiento de kardex y capital.
- `registrarStockInicial(producto, observacion)`: usado tras `crearProducto` cuando se define stock>0; registra ENTRADA en kardex y egreso en capital.

### KardexService (`services/kardex/KardexService.java`)
Servicio de auditoría de inventario.

- `buscar(pageable, productoId, usuarioId, tipo, desde, hasta)`: construye especificación con filtros y devuelve `Page<KardexResponse>`.
- `registrarMovimiento(producto, cantidad, comentario, tipo)`: crea registro con usuario autenticado (vía `UsuarioService.obtenerUsuarioActual`).

### ProductoService (`services/producto/ProductoService.java`)
Orquesta creación, actualización y consulta agrupada de productos, apoyándose en `InventarioService`.

- `buscarPorId(id)`: acceso directo a repositorio.
- `traerTodos(nombreFiltro, page, size)`: usa `ProductoSpecification` y realiza agrupamiento manual por nombre para devolver `PaginaProductoResponse` con variantes (por precio). El objeto final incluye `stockTotal`, `cantidadPorCajas` y lista de variantes con id/stock/precio.
- `crearProducto(productoRequest)`: valida duplicados, transforma DTO y persiste. Si trae stock inicial, delega a `inventarioService.registrarStockInicial`.
- `actualizarProducto(id, request)`: asegura que el stock no cambie (protege contra ajustes directos), valida unicidad del nombre y actualiza campos editables mediante el mapper.
- `registrarIngresoProductos(lista)` / `eliminarCantidad(id, cantidad, observacion)`: wrappers que delegan en `InventarioService` y devuelven DTOs.

### UsuarioService (`services/usuario/UsuarioService.java`)
Gestión de usuarios y acceso al contexto de seguridad.

- Métodos CRUD: `listarUsuarios`, `obtenerPorId`, `obtenerPorUsuario`, `crear` (codifica contraseña), `actualizar` (opcionalmente reemplaza contraseña) y `eliminar`.
- `obtenerUsuarioActual()`: lee el `SecurityContextHolder` y retorna el `Usuario` de `UsuarioDetails`; lanza excepción si no hay autenticación.

### CustomUserDetailsService y UsuarioDetails (`services/security`)
Puente con Spring Security.

- `CustomUserDetailsService.loadUserByUsername`: consulta `UsuarioRepository` por `usuario` y devuelve `UsuarioDetails`.
- `UsuarioDetails`: implementación de `UserDetails` que expone credenciales y asigna rol fijo `ROLE_USER`.

### VentaService (`services/venta/VentaService.java`)
Núcleo transaccional que vincula clientes, productos, inventario, cartera y capital.

- `traerTodos(pageable, nombreProducto, tipoVenta, nombreCliente, nombreUsuario, activa, desde, hasta)`: trabaja sobre `DetalleVentaRepository` para permitir filtros combinados por producto, tipo, cliente, vendedor, estado y rango de fechas usando `DetalleVentaSpecification`. Responde con `VentaDetalleTablaResponse`.
- `crearVenta(request)`: valida lista de items, obtiene usuario autenticado y cliente, usa `VentaMapper` para armar entidad (inyecta productos vía `ProductoService.buscarPorId`) y persiste. Luego:
  1. Llama `ajustarInventarioPorVenta` para registrar SALIDAS por cada detalle (usa `InventarioService.registrarSalida`).
  2. Registra movimiento de capital diferenciando contado vs crédito.
  3. Llama `CarteraService.registrarVentaEnCartera` para abrir deuda cuando corresponda.
- `anularVenta(ventaId, razon)`: valida id y motivo, asegura que la venta esté activa, y ejecuta:
  1. `revertirInventarioPorAnulacion`: por cada detalle crea entrada (usa `inventarioService.registrarMovimiento`).
  2. Marca la venta como inactiva con mensaje detallado.
  3. Invoca `CapitalService.revertirVenta` para inverter el impacto financiero.
  4. Usa `CarteraService.ajustarPorAnulacion` para cerrar deuda y `registrarMovimiento` tipo `AJUSTE`.
  5. Persiste la venta actualizada y retorna DTO.
- Métodos auxiliares: `parseTipoVenta`, `ajustarInventarioPorVenta`, `revertirInventarioPorAnulacion`.

### Clases de especificación
- `CapitalMovimientoSpecification`, `CarteraSpecification`, `CarteraMovimientoSpecification`, `ClienteSpecification`, `ProductoSpecification`, `KardexSpecification`, `DetalleVentaSpecification`, `VentaSpecification`: cada una encapsula filtros `Specification<T>` para construir consultas dinámicas (búsqueda por nombre, tipo, fecha, estado, etc.).

---

## Conclusiones rápidas
- Los modelos cubren capital, cartera, ventas, inventario y seguridad; cada entidad tiene validaciones básicas (`@NotNull`, `@Positive`, etc.) y eventos `@PrePersist`.
- Los servicios siguen una cadena coherente: ventas impactan inventario, capital y cartera; inventario sincroniza kardex y capital; cartera sincroniza capital en abonos/anulaciones.
- La mayoría de los filtros de consultas están encapsulados en clases `Specification`, lo que facilita auditorías personalizadas sin duplicar lógica.

Este documento debe servir como base para verificar reglas de negocio, validar pruebas pendientes o extender la funcionalidad actual.
