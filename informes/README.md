# ms-informes

Microservicio del dominio **Informes** de Chibson Instruments:

- **Dashboard** (HU-50 a HU-56): tarjetas KPI, resumen rápido, gráficos y tablas.
- **Informes** (HU-45 a HU-47): ventas por periodo, estado de pedidos y productos más vendidos.

Puerto **8086**. **No tiene base de datos:** cada vez que se pide algo, consulta a los otros
microservicios y calcula el resultado. Por eso los datos siempre están actualizados.

El registro de ventas (HU-57 a HU-59) está en ms-ventas.

## Cómo ejecutarlo

1. Levantar **ms-usuarios** (8081), **ms-productos** (8082), **ms-ventas** (8083) y **ms-soporte** (8084).
2. Ejecutar `InformesApplication`.
3. Para tener datos, ejecutar antes la colección de Postman de ms-ventas.
4. Importar `chibson-informes.postman_collection.json` y probar.

> En el `pom.xml` se quitaron JPA, H2 y el conector de MySQL que venían del Initializr, porque este
> microservicio no guarda nada. Sin ellos Spring no intenta conectarse a una base de datos.

## Estructura

```
chibson.informes
├── model        Dashboard, Informe, TipoInforme, Periodo   (clases simples, no son entidades)
├── service      DashboardService, InformeService, Calculos, Permisos
├── controller   DashboardController, InformeController
├── dto          PedidoDTO, DetalleDTO, ProductoDTO          (lo que llega de otros microservicios)
│                PuntoGraficoDTO, ProductoVendidoDTO, StockBajoDTO (respuestas de gráficos y tablas)
├── client       VentasClient, ProductosClient, UsuariosClient, SoporteClient
└── config       RestTemplateConfig
```

## De dónde sale cada dato

| Dato | Microservicio | Endpoint |
|---|---|---|
| Pedidos con fecha, estado, total, región y líneas | ms-ventas | `GET /api/pedidos/resumen?desde=&hasta=` |
| Devoluciones pendientes | ms-ventas | `GET /api/devoluciones/pendientes/contar` |
| Productos con stock bajo | ms-productos | `GET /api/inventario/bajo-stock` |
| Clientes registrados hoy | ms-usuarios | `GET /api/usuarios/registrados-hoy` |
| Tickets sin resolver | ms-soporte | `GET /api/tickets/abiertos/contar` |
| Permiso `VER_INFORMES` | ms-usuarios | `GET /api/roles/verificar` |

## Reglas de cálculo

- **Venta:** un pedido con pago aprobado que no fue cancelado. Los cancelados no suman, porque su stock se repuso.
- **Montos:** el total de cada venta incluye el envío. Ventas por categoría y productos más vendidos
  usan el subtotal de cada línea (precio × cantidad), sin envío.
- **Ticket promedio:** total vendido dividido por el número de ventas.
- **Variación del mes (HU-50):** compara del día 1 a hoy con el mismo rango de días del mes anterior.
  Si el mes anterior no tuvo ventas, queda null y el frontend muestra "sin comparación".
- **Pedidos pendientes:** pedidos pagados que todavía no se despachan (PAGADO o EN_PROCESO).
- **Stock bajo (HU-56):** `nivel` es CRITICO si el producto está agotado o tiene la mitad de su mínimo
  o menos, y BAJO en otro caso. El frontend lo pinta rojo o amarillo.
- **Si un microservicio no responde:**
  - Sin ms-ventas no hay informe ni dashboard: responde 503.
  - Tickets, clientes nuevos, devoluciones y stock bajo de las tarjetas quedan en null y el resto
    del dashboard se muestra igual.

## Endpoints (todos con permiso VER_INFORMES)

| Método | Ruta | HU |
|---|---|---|
| GET | `/api/dashboard/kpis` | 50, 52 |
| GET | `/api/dashboard/ventas-7-dias` | 51 |
| GET | `/api/dashboard/ventas-categoria` | 53 |
| GET | `/api/dashboard/top-productos` | 54 |
| GET | `/api/dashboard/pedidos-region` | 55 |
| GET | `/api/dashboard/stock-bajo` | 56 |
| GET | `/api/informes/ventas?periodo=&desde=&hasta=` | 45 |
| GET | `/api/informes/pedidos?desde=&hasta=` | 46 |
| GET | `/api/informes/productos?desde=&hasta=&categoria=` | 47 |

- **periodo:** `DIARIO`, `MENSUAL` o `ANUAL` toman el día, mes o año de `desde` (hoy si no se indica).
  `PERSONALIZADO` necesita `desde` y `hasta`. Las filas van por día, o por mes si el rango supera los 62 días.
- **Informe de pedidos:** sin fechas incluye todos los pedidos. Cada fila trae los números de pedido
  para abrir su detalle en `GET /api/pedidos/{numero}` de ms-ventas.
- **Productos más vendidos:** sin fechas usa el mes en curso.
- Cuando un informe no tiene datos, el campo `mensaje` lo indica (HU-45 criterio 3).

## Lo que hace el frontend

Algunos criterios son de presentación y quedan para las páginas HTML:

- Tarjetas clickeables (HU-50 c3), colores e íconos de la variación (HU-50 c2) y los textos de "0"
  (HU-50 c4, HU-52 c3).
- Formato CLP en los gráficos (HU-51 c2, HU-53 c2) y el mensaje "No hay ventas registradas en los
  últimos 7 días" cuando los 7 puntos están en 0 (HU-51 c3).
- Enlaces a los listados completos (HU-54 c2, HU-55 c3, HU-56 c3).

## Fuera de alcance

- HU-48 y HU-49 (exportar a PDF y Excel) y HU-60 a HU-62 (informes guardados), porque necesitan
  generar archivos, que el curso aún no ve. Por eso `Informe` no se guarda.
