# ms-ventas

Microservicio del dominio **Ventas** de Chibson Instruments:

- **Carrito** (EP3): HU-20 a HU-22.
- **Compra y pago simulado** (EP3): HU-23 a HU-25.
- **Pedidos** (EP5): detalle, estados, historial, notificaciones, cancelación y eliminación. HU-33 a HU-38 y HU-44.
- **Devoluciones** (EP5): HU-39 a HU-43.
- **Registro de ventas** (EP6): HU-57 a HU-59.

Puerto **8083**, base de datos MySQL **chibson_ventas**.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_ventas` se crea sola.
2. Revisar usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Levantar **ms-usuarios** (8081) y **ms-productos** (8082). Ventas los necesita para validar al
   cliente, consultar precios y mover el stock.
4. Ejecutar `VentasApplication`.
5. Importar `chibson-ventas.postman_collection.json` en Postman y ejecutar las carpetas en orden.
   La carpeta 0 crea un cliente y dos productos de prueba, y los números de pedido se guardan solos en variables.

## Estructura

```
chibson.ventas
├── model        Carrito, ItemCarrito                                  (carrito)
│                Pedido, DetallePedido, HistorialEstadoPedido, Pago     (pedidos)
│                SolicitudDevolucion                                    (devoluciones)
│                EstadoPedido, MetodoPago, EstadoPago, MotivoDevolucion, EstadoDevolucion
├── repository   CarritoRepository, PedidoRepository, DevolucionRepository, ...
├── service      CarritoService, PedidoService, DevolucionService, VentaService, Permisos
├── controller   CarritoController, PedidoController, DevolucionController, VentaController
├── dto          CarritoDTO, CheckoutDTO, PagoDTO, PedidoDTO, CompraRespuestaDTO, VentaDTO, ...
├── client       ProductosClient (precio y stock), UsuariosClient (permisos, datos, notificaciones, historial)
└── config       RestTemplateConfig
```

## Flujo de una compra

1. El cliente arma su carro (`/api/carrito`). Al agregar se valida que el producto esté activo
   y que la cantidad no supere el stock.
2. `POST /api/pedidos/checkout` crea el pedido **PENDIENTE**, vuelve a consultar precio y stock
   y pasa por la pasarela simulada:
   - **Aprobado (201):** se descuenta el stock en ms-productos, el pedido pasa a **PAGADO**,
     el carro se vacía y el cliente recibe una notificación. La respuesta es el comprobante.
   - **Rechazado (402):** queda un pago RECHAZADO, el pedido sigue PENDIENTE y el carro se mantiene.
     Se reintenta con `POST /api/pedidos/{numero}/pagar`.
3. El administrador avanza el estado: PAGADO → EN_PROCESO → ENVIADO → ENTREGADO. Cada cambio
   queda en el historial del pedido, se notifica al cliente (HU-38) y se registra en el historial de acciones.
   PENDIENTE significa "esperando pago": solo sale de ahí al pagar o al cancelar.
4. Con el pedido ENTREGADO el cliente puede pedir una devolución, y ms-productos le permite reseñar.

## Reglas principales

- **Dirección (HU-25):** por defecto se usa la del perfil. Si el cliente marca
  `usarDireccionDistinta`, se guarda en el pedido solo para ese envío y no se toca el perfil.
  Región, ciudad o comuna y número son obligatorios; condominio es opcional.
- **Montos:** los precios incluyen IVA. `iva` es informativo (19/119 del subtotal) y
  `total = subtotal + costoEnvio`.
- **Stock:** si una línea no se puede descontar (por ejemplo, otro cliente compró la última unidad),
  se repone lo ya descontado y la compra responde 409.
- **Cancelar (HU-37):** se puede antes de ENTREGADO y pide un motivo. Si estaba pagado, el stock
  vuelve con el movimiento **CANCELACION**.
- **Eliminar (HU-44):** solo pedidos sin pago aprobado. Los facturados se cancelan.
- **Devoluciones (HU-39 a HU-43):** solo pedidos ENTREGADOS, dentro de 10 días desde la entrega,
  una solicitud activa por producto y pedido. Al aprobarla, el stock vuelve con el movimiento **DEVOLUCION**.
- **Ventas (HU-57):** una venta es un pedido con pago aprobado; el número de venta es el número de pedido.
- **Seguridad:** el RUT del usuario llega en el encabezado `X-Rut-Usuario`.
  - Cliente: ms-usuarios debe confirmar que existe y está activo (si no, 401).
  - Administrador: necesita `GESTIONAR_PEDIDOS` (pedidos y devoluciones) o `VER_VENTAS` (ventas).
  - Un cliente solo ve sus propios pedidos; los de otro responden 404.
- **Si otro microservicio no responde:** se responde 503 cuando hace falta su respuesta
  (precio, stock, permisos). Las notificaciones y el historial de acciones no detienen la operación.

## Endpoints

| Método | Ruta | Quién | HU |
|---|---|---|---|
| GET | `/api/carrito` | Cliente | 20 a 22 |
| POST | `/api/carrito/items` `{sku, cantidad}` | Cliente | 20 |
| PUT | `/api/carrito/items/{idItem}` `{cantidad}` | Cliente | 21 |
| DELETE | `/api/carrito/items/{idItem}` | Cliente | 22 |
| POST | `/api/pedidos/checkout` | Cliente | 23, 24, 25 |
| POST | `/api/pedidos/{numero}/pagar` `{metodoPago}` | Cliente | 23 |
| GET | `/api/pedidos/mis-pedidos` y `/mis-pedidos/{numero}` | Cliente | 33, 36 |
| GET | `/api/pedidos?estado=` | GESTIONAR_PEDIDOS | 34 |
| GET | `/api/pedidos/{numero}` | GESTIONAR_PEDIDOS | 34, 36 |
| PATCH | `/api/pedidos/{numero}/estado` `{estado}` | GESTIONAR_PEDIDOS | 35, 38 |
| PATCH | `/api/pedidos/{numero}/cancelar` `{motivo}` | GESTIONAR_PEDIDOS | 37 |
| DELETE | `/api/pedidos/{numero}` | GESTIONAR_PEDIDOS | 44 |
| POST | `/api/devoluciones` | Cliente | 39 |
| GET | `/api/devoluciones/mis-devoluciones` | Cliente | 43 |
| GET | `/api/devoluciones?estado=` y `/{numero}` | GESTIONAR_PEDIDOS | 40 |
| PATCH | `/api/devoluciones/{numero}/aprobar` | GESTIONAR_PEDIDOS | 41 |
| PATCH | `/api/devoluciones/{numero}/rechazar` `{motivo}` | GESTIONAR_PEDIDOS | 42 |
| GET | `/api/ventas?texto=&metodo=&desde=&hasta=` | VER_VENTAS | 57, 58 |
| GET | `/api/ventas/{numero}` | VER_VENTAS | 59 |
| GET | `/api/pedidos/cliente/{rut}/existe` | ms-usuarios | 17 |
| GET | `/api/pedidos/producto/{sku}/existe` | ms-productos | 04 |
| GET | `/api/pedidos/cliente/{rut}/recibio/{sku}` | ms-productos | 81 |
| GET | `/api/pedidos/resumen?desde=&hasta=` | ms-informes | 45 a 55 |
| GET | `/api/devoluciones/pendientes/contar` | ms-informes | 52 |

Las fechas de `/api/ventas` van como `AAAA-MM-DD`. La paginación del listado de ventas (HU-57)
se hace en el frontend: el endpoint devuelve la lista completa ya ordenada.

## Supuestos (la plantilla no los define)

- **Costo de envío fijo** de $5.000, configurable en `ventas.costo-envio`.
- **Pago simulado:** `simularRechazo: true` en el checkout o en el reintento hace que la pasarela rechace el pago.
- **Plazo de devolución** de 10 días desde la entrega (el ejemplo de HU-39).
- **Métodos de pago:** DEBITO, CREDITO y TRANSFERENCIA, como dice HU-23. HU-58 menciona "tarjeta, efectivo,
  transferencia"; conviene unificarlo en la plantilla.
- **Estado REEMBOLSADA:** existe en el enum, pero todavía no tiene una acción que lo asigne.
- **`ItemCarrito.nombreProducto`:** campo nuevo respecto del diagrama, para mostrar el carro sin
  consultar a ms-productos cada vez.
