# ms-informes

Microservicio del dominio **Reportes** de Chibson Instruments (EP6):

- **Reportes manuales** (HU-45 a HU-48): ventas por periodo, estado de pedidos y productos más vendidos.
- **Resumen general automático** (HU-49, HU-50): uno por semana y uno por mes.
- **Reportes guardados** (HU-60 a HU-62): listado, detalle y eliminación.

Puerto **8086**, base de datos MySQL **chibson_informes**. Cada reporte se guarda con sus filas tal como
quedó al generarse, así que no cambia aunque después cambien las ventas (HU-61).

El registro de ventas (HU-57 a HU-59) está en ms-ventas.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_informes` se crea sola.
2. Levantar **ms-usuarios** (8081), **ms-productos** (8087), **ms-ventas** (8083) y **ms-soporte** (8084).
3. Ejecutar `InformesApplication`. Al arrancar genera los resúmenes automáticos que falten (ver abajo).
4. Para tener datos, ejecutar antes la colección de Postman de ms-ventas.
5. Importar `chibson-informes.postman_collection.json` y ejecutar las carpetas en orden.

## Estructura

```
chibson.informes
├── model        Reporte, FilaReporte, TipoReporte, Periodo, OrigenReporte
├── repository   ReporteRepository
├── service      ReporteService (guardar, listar, ver, eliminar), GeneradorReportes (contenido de cada tipo),
│                ReportesAutomaticos (@Scheduled y pendientes al iniciar), Calculos, Permisos
├── controller   ReporteController
├── dto          PedidoDTO, ItemDTO, ReembolsoDTO, ProductoDTO   (lo que llega de otros microservicios)
│                GenerarReporteDTO, ReporteListadoDTO, ProductoVendidoDTO
├── client       VentasClient, ProductosClient, UsuariosClient, SoporteClient
└── config       RestTemplateConfig
```

## Endpoints (todos con permiso VER_INFORMES)

| Método | Ruta | HU |
|---|---|---|
| POST | `/api/reportes` `{tipo, periodo, desde, hasta, categoria}` | 45 a 48 |
| GET | `/api/reportes?tipo=&origen=&desde=&hasta=` | 60 |
| GET | `/api/reportes/{id}` | 61 |
| DELETE | `/api/reportes/{id}` | 62 |

Ejemplo:

```json
{ "tipo": "PRODUCTOS_MAS_VENDIDOS", "periodo": "MENSUAL", "desde": "2026-10-01", "categoria": "GUITARRA" }
```

- **tipo:** `VENTAS_POR_PERIODO`, `ESTADO_PEDIDOS` o `PRODUCTOS_MAS_VENDIDOS`. `RESUMEN_GENERAL` responde 400
  porque solo es automático (HU-50 criterio 6).
- **periodo:** `DIARIO`, `SEMANAL`, `MENSUAL` o `ANUAL` toman el día, la semana (lunes a domingo), el mes o
  el año de `desde` (hoy si no viene). `PERSONALIZADO` necesita `desde` y `hasta`.
- **categoria:** opcional, solo para productos más vendidos.
- En el listado, `desde` y `hasta` filtran por la fecha de creación del reporte.
- La confirmación antes de eliminar (HU-62) la pide el frontend.

## Qué trae cada reporte

Los datos generales van en el reporte (`totalVentas`, `numeroPedidos`, `ticketPromedio`, `mensaje`) y el
detalle en `filas`. Cada fila tiene `seccion`, `etiqueta`, `cantidad`, `monto`, y a veces `valor` (texto) y
`referencia` (número de pedido o SKU para abrir el detalle).

| Tipo | Secciones de las filas |
|---|---|
| Ventas por periodo | `TOTALES` (brutas, reembolsos, netas) y `POR_DIA`, o `POR_MES` si el rango supera 62 días |
| Estado de pedidos | `RESUMEN` (cantidad y monto por estado) y una sección por estado con cada pedido |
| Productos más vendidos | `RANKING` (unidades e ingresos netos; categoría en `valor`, SKU en `referencia`) |
| Resumen general | `INDICADORES`, `TOP_5`, `POR_CATEGORIA`, `POR_REGION` y `STOCK_BAJO` |

Si el periodo no tiene datos, el reporte se guarda igual y `mensaje` lo indica (HU-45 c3, HU-48 c4).

## Resumen general automático (HU-49)

- **Cada lunes a las 00:05** genera el de la semana anterior (lunes a domingo).
- **El día 1 a las 00:10** genera el del mes anterior.
- **Al iniciar** genera los de las semanas y meses ya cerrados que falten, desde el primer pedido
  registrado. Cubre el caso de que el servicio estuviera apagado a la hora programada.
- Nunca repite un periodo que ya tiene su resumen automático.
- Se programa con `@Scheduled` y necesita `@EnableScheduling` en `InformesApplication`. Las horas son de
  Chile (`America/Santiago`).
- Si ms-ventas no responde, el error queda en el log y ese periodo se genera la próxima vez que arranque.
- `reportes.automaticos.activos=false` en `application.properties` lo desactiva (las pruebas lo usan así).

Con datos creados hoy no aparece ningún resumen todavía, porque la semana en curso no ha cerrado: el
primero se genera el lunes siguiente o al reiniciar el servicio después de ese lunes.

## Reglas de cálculo

- **Venta:** un pedido con pago aprobado que no fue cancelado. Se cuenta por la fecha del pedido.
- **Reembolsos (HU-45 c4):** se descuentan en el periodo de la **fecha del reembolso**, no en el de la
  compra. ms-ventas entrega los pedidos del rango y también los anteriores que tuvieron un reembolso en
  él. Así un reporte ya generado no cambia cuando se reembolsa algo de un periodo pasado.
- **Montos:** el total de cada venta incluye el envío. Productos más vendidos y ventas por categoría usan
  el subtotal de cada línea (precio × cantidad), sin envío. Un producto que queda en 0 unidades netas no
  aparece en el ranking.
- **Ticket promedio:** ventas netas divididas por el número de ventas.
- **Variación (HU-50):** ventas netas del periodo contra las de la semana o el mes anterior. Si el anterior
  no tuvo ventas, `valor` dice "Sin ventas en el periodo anterior".
- **Indicadores del momento:** pedidos pendientes de despacho (PAGADO o EN_PROCESO), stock bajo, tickets
  abiertos y devoluciones pendientes son los que había al generar el reporte.
- **Si un microservicio no responde:** sin ms-ventas no hay reporte (503 en los manuales). En el resumen
  general, los indicadores de usuarios, productos o soporte quedan con el texto "Sin datos: ... no
  respondió", nunca vacíos (HU-50 c4).

## De dónde sale cada dato

| Dato | Microservicio | Endpoint |
|---|---|---|
| Pedidos con fecha, estado, total, región, productos y reembolsos | ms-ventas | `GET /api/pedidos/resumen?desde=&hasta=` |
| Devoluciones pendientes | ms-ventas | `GET /api/devoluciones/pendientes/contar` |
| Productos con stock bajo | ms-productos | `GET /api/productos/bajo-stock` |
| Clientes registrados en el periodo | ms-usuarios | `GET /api/usuarios/registrados/contar?desde=&hasta=` |
| Tickets sin resolver | ms-soporte | `GET /api/tickets/abiertos/contar` |
| Permiso `VER_INFORMES` | ms-usuarios | `GET /api/roles/verificar` |
