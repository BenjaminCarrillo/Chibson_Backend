# ms-ventas

Microservicio del dominio **Ventas** de Chibson Instruments:

- **Compra y pago simulado** (EP3): HU-23 a HU-25. El carro vive en el frontend; el backend recibe
  los productos y cantidades al pagar.
- **Pedidos** (EP5): detalle, estados, historial, notificaciones, cancelación y eliminación. HU-33 a HU-38 y HU-44.
- **Devoluciones y reembolso** (EP5): HU-39 a HU-43 y HU-89.
- **Registro de ventas** (EP6): HU-57 a HU-59, con montos netos de reembolsos.

Puerto **8083**, base de datos MySQL **chibson_ventas**.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_ventas` se crea sola.
2. Revisar usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Levantar **ms-usuarios** (8081) y **ms-productos** (8087). Ventas los necesita para validar al
   cliente, consultar precios y mover el stock.
4. Ejecutar `VentasApplication`.
5. Importar `chibson-ventas.postman_collection.json` en Postman y ejecutar las carpetas en orden.
   La carpeta 0 crea un cliente y dos productos de prueba, y los números de pedido y de devolución se
   guardan solos en variables.

## Estructura

```
chibson.ventas
├── model        Pedido, ItemPedido, HistorialEstadoPedido, Pago        (pedidos)
│                SolicitudDevolucion, ItemDevolucion                    (devoluciones)
│                EstadoPedido, MetodoPago, EstadoPago, MotivoDevolucion, EstadoDevolucion
├── repository   PedidoRepository, DevolucionRepository, HistorialEstadoRepository, PagoRepository
├── service      PedidoService, DevolucionService, VentaService, Permisos
├── controller   PedidoController, DevolucionController, VentaController
├── dto          CheckoutDTO, ItemDTO, PagoDTO, PedidoDTO, CompraRespuestaDTO, DevolucionDTO,
│                VentaDTO, ReembolsoDTO, PedidoResumenDTO, ...
├── client       ProductosClient (precio y stock), UsuariosClient (permisos, datos, notificaciones, historial)
└── config       RestTemplateConfig
```

## Flujo de una compra

1. El cliente arma su carro en el frontend (no hay carrito en el backend).
2. `POST /api/pedidos/checkout` recibe `items: [{sku, cantidad}]`, crea el pedido **PENDIENTE**,
   consulta a ms-productos el precio y el stock actuales y pasa por la pasarela simulada:
   - **Aprobado (201):** se descuenta el stock en ms-productos, el pedido pasa a **PAGADO** y el
     cliente recibe una notificación. La respuesta es el comprobante.
   - **Rechazado (402):** queda un pago RECHAZADO y el pedido sigue PENDIENTE; el frontend conserva
     el carro. Se reintenta con `POST /api/pedidos/{numero}/pagar`.
3. El administrador avanza el estado: PAGADO → EN_PROCESO → ENVIADO → ENTREGADO. Cada cambio
   queda en el historial del pedido, se notifica al cliente (HU-38) y se registra en el historial de acciones.
   PENDIENTE significa "esperando pago": solo sale de ahí al pagar o al cancelar.
4. Con el pedido ENTREGADO el cliente puede pedir una devolución, y ms-productos le permite reseñar.

## Flujo de una devolución

1. El cliente envía `POST /api/devoluciones` con uno o varios productos del pedido y cuántas unidades
   de cada uno devuelve. Recibe un número de seguimiento. Estado **PENDIENTE**.
2. El administrador la **aprueba** (se repone el stock de esos productos y cantidades) o la
   **rechaza** con un motivo. El cliente recibe una notificación en ambos casos.
3. Cuando devuelve el dinero, la marca como **reembolsada** (HU-89). El monto se calcula solo con las
   unidades devueltas y el precio que el cliente pagó; queda la fecha, el monto y el administrador.
   Es un estado final.
4. Desde ese momento la venta muestra el monto reembolsado y su total neto, y ms-informes lo descuenta
   en el periodo de la fecha del reembolso.

## Reglas principales

- **Dirección (HU-25):** por defecto se usa la del perfil. Si el cliente marca
  `usarDireccionDistinta`, se guarda en el pedido solo para ese envío y no se toca el perfil.
  Región, ciudad o comuna y número son obligatorios; condominio es opcional.
- **Montos:** los precios incluyen IVA. `iva` es informativo (19/119 del subtotal) y
  `total = subtotal + costoEnvio`.
- **Stock:** si una línea no se puede descontar (por ejemplo, otro cliente compró la última unidad),
  se repone lo ya descontado y la compra responde 409.
- **Cancelar (HU-37):** se puede antes de ENTREGADO y pide un motivo. Si estaba pagado, se repone el stock.
- **Eliminar (HU-44):** solo pedidos sin pago aprobado. Los facturados se cancelan.
- **Devoluciones (HU-39):** solo pedidos ENTREGADOS y dentro de 10 días desde la entrega. De cada
  producto no se pueden devolver más unidades de las compradas, contando las solicitudes anteriores
  no rechazadas del mismo pedido. Si un SKU viene repetido se suman las cantidades.
- **Reembolso (HU-89):** solo desde APROBADA. Después la solicitud no cambia más.
- **Ventas (HU-57):** una venta es un pedido con pago aprobado; el número de venta es el número de
  pedido. `totalNeto = total − montoReembolsado`.
- **Seguridad:** el RUT del usuario llega en el encabezado `X-Rut-Usuario`.
  - Cliente: ms-usuarios debe confirmar que existe y está activo (si no, 401).
  - Administrador: necesita `GESTIONAR_PEDIDOS` (pedidos y devoluciones) o `VER_VENTAS` (ventas).
  - Un cliente solo ve sus propios pedidos; los de otro responden 404.
- **Si otro microservicio no responde:** se responde 503 cuando hace falta su respuesta
  (precio, stock, permisos). Las notificaciones y el historial de acciones no detienen la operación.

## Endpoints

| Método | Ruta | Quién | HU |
|---|---|---|---|
| POST | `/api/pedidos/checkout` `{items, metodoPago, usarDireccionDistinta, ...}` | Cliente | 23, 24, 25 |
| POST | `/api/pedidos/{numero}/pagar` `{metodoPago}` | Cliente | 23 |
| GET | `/api/pedidos/mis-pedidos` y `/mis-pedidos/{numero}` | Cliente | 33, 36 |
| GET | `/api/pedidos?estado=` | GESTIONAR_PEDIDOS | 34 |
| GET | `/api/pedidos/{numero}` | GESTIONAR_PEDIDOS | 34, 36 |
| PATCH | `/api/pedidos/{numero}/estado` `{estado}` | GESTIONAR_PEDIDOS | 35, 38 |
| PATCH | `/api/pedidos/{numero}/cancelar` `{motivo}` | GESTIONAR_PEDIDOS | 37 |
| DELETE | `/api/pedidos/{numero}` | GESTIONAR_PEDIDOS | 44 |
| POST | `/api/devoluciones` `{numeroPedido, items, motivo, descripcion, evidenciaUrl}` | Cliente | 39 |
| GET | `/api/devoluciones/mis-devoluciones` | Cliente | 43 |
| GET | `/api/devoluciones?estado=` y `/{numero}` | GESTIONAR_PEDIDOS | 40 |
| PATCH | `/api/devoluciones/{numero}/aprobar` | GESTIONAR_PEDIDOS | 41 |
| PATCH | `/api/devoluciones/{numero}/rechazar` `{motivo}` | GESTIONAR_PEDIDOS | 42 |
| PATCH | `/api/devoluciones/{numero}/reembolsar` | GESTIONAR_PEDIDOS | 89 |
| GET | `/api/ventas?texto=&metodo=&desde=&hasta=` | VER_VENTAS | 57, 58 |
| GET | `/api/ventas/{numero}` | VER_VENTAS | 59 |
| GET | `/api/pedidos/cliente/{rut}/existe` | ms-usuarios | 17 |
| GET | `/api/pedidos/producto/{sku}/existe` | ms-productos | 04 |
| GET | `/api/pedidos/cliente/{rut}/recibio/{sku}` | ms-productos | 81 |
| GET | `/api/pedidos/resumen?desde=&hasta=` | ms-informes | 45 a 50 |
| GET | `/api/devoluciones/pendientes/contar` | ms-informes | 50 |

Ejemplo de checkout:

```json
{
  "items": [{ "sku": "GTR-0001", "cantidad": 2 }, { "sku": "PED-0001", "cantidad": 1 }],
  "metodoPago": "CREDITO",
  "usarDireccionDistinta": false
}
```

Ejemplo de solicitud de devolución:

```json
{
  "numeroPedido": 1,
  "items": [{ "sku": "GTR-0001", "cantidad": 1 }, { "sku": "PED-0001", "cantidad": 1 }],
  "motivo": "PRODUCTO_DEFECTUOSO",
  "descripcion": "La pastilla del puente no suena.",
  "evidenciaUrl": "img/evidencia1.jpg"
}
```

Las fechas de `/api/ventas` van como `AAAA-MM-DD`. La paginación del listado de ventas (HU-57)
se hace en el frontend: el endpoint devuelve la lista completa ya ordenada.

`/api/pedidos/resumen` devuelve los pedidos creados en el rango y también los pedidos anteriores que
tuvieron un reembolso en ese rango. Los montos de cada pedido son brutos; sus reembolsos vienen aparte,
con su fecha, para que ms-informes los descuente en el periodo correcto.

## Supuestos (la plantilla no los define)

- **Costo de envío fijo** de $5.000, configurable en `ventas.costo-envio`. El reembolso cubre solo los
  productos devueltos, no el envío.
- **Pago simulado:** `simularRechazo: true` en el checkout o en el reintento hace que la pasarela rechace el pago.
- **Plazo de devolución** de 10 días desde la entrega (el ejemplo de HU-39).
- **Métodos de pago:** DEBITO, CREDITO y TRANSFERENCIA, como dice HU-23. HU-58 menciona "tarjeta, efectivo,
  transferencia"; conviene unificarlo en la plantilla.
