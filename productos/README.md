# ms-productos

Microservicio del dominio **Productos** de Chibson Instruments, con tres módulos:

- **Catálogo** (EP1): productos, categoría, SKU y ficha técnica. HU-01 a HU-07 y HU-19.
- **Inventario** (EP7): stock, stock mínimo y movimientos. HU-63 y HU-64.
- **Reseñas** (EP11): calificaciones, moderación y respuestas. HU-81 a HU-88.

Puerto **8082**, base de datos MySQL **chibson_productos**.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_productos` se crea sola.
2. Revisar usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Levantar **ms-usuarios** (8081): las acciones de administración le preguntan los permisos.
4. Ejecutar `ProductosApplication`.
5. Importar `chibson-productos.postman_collection.json` en Postman y probar en orden.

## Estructura

```
chibson.productos
├── model        Producto, Categoria, EstadoProducto        (catálogo)
│                Inventario, MovimientoStock, TipoMovimiento (inventario)
│                Resena, EstadoResena                        (reseñas)
├── repository   uno por entidad
├── service      ProductoService, InventarioService, ResenaService, FichaTecnica
├── controller   ProductoController, InventarioController, ResenaController
├── dto          ProductoDTO, ProductoRespuestaDTO, InventarioDTO, MovimientoDTO, ResenaDTO, ...
├── client       UsuariosClient (permisos, nombre del cliente, historial), VentasClient (pedidos)
└── config       RestTemplateConfig
```

`ProductoService` llama a `InventarioService` directamente en Java: crear o editar un producto
y su stock ocurre en una sola transacción.

## Reglas principales

- **SKU automático:** prefijo de la categoría y correlativo, por ejemplo `GTR-0001`, `TEC-0001`.
- **Stock:** vive en `Inventario`, no en `Producto`. "Agotado" es stock 0; "bajo stock" es stock igual o menor al mínimo (5 si no se indica).
- **Movimientos:** cada cambio de stock queda registrado con tipo (`VENTA`, `DEVOLUCION`, `CANCELACION`, `AJUSTE_MANUAL`), cantidad (negativa en salidas), saldo y número de pedido.
- **Ficha técnica:** texto `clave:valor;clave:valor`. Al guardar se valida y el error indica qué segmento falló. Al mostrar se entrega separada en filas, omitiendo segmentos mal formados.
- **Reseñas:** solo si el cliente compró y recibió el producto, una por producto. Nacen `PENDIENTE` y solo las `APROBADA` son públicas. El promedio se calcula al consultar.

## Endpoints

| Método | Ruta | Quién | HU |
|---|---|---|---|
| GET | `/api/productos` | Público | HU-03 |
| GET | `/api/productos/{sku}` | Público | HU-19 |
| GET | `/api/productos/categorias` | Público | HU-07 |
| GET | `/api/productos/admin?estado=&categoria=&texto=` | GESTIONAR_PRODUCTOS | — |
| GET | `/api/productos/admin/{sku}` | GESTIONAR_PRODUCTOS | — |
| POST | `/api/productos` | GESTIONAR_PRODUCTOS | HU-01 |
| PUT | `/api/productos/{sku}` | GESTIONAR_PRODUCTOS | HU-02 |
| PATCH | `/api/productos/{sku}/desactivar` y `/reactivar` | GESTIONAR_PRODUCTOS | HU-03 |
| DELETE | `/api/productos/{sku}` | GESTIONAR_PRODUCTOS | HU-04 |
| POST / PUT | `/api/productos/{sku}/ficha-tecnica` | GESTIONAR_PRODUCTOS | HU-05, HU-06 |
| GET | `/api/inventario/{sku}/movimientos` | GESTIONAR_PRODUCTOS | HU-64 |
| POST | `/api/resenas` | Cliente con sesión | HU-81 |
| GET | `/api/resenas/producto/{sku}` | Público | HU-82 |
| GET | `/api/resenas/admin?estado=&sku=&calificacion=` | GESTIONAR_RESENAS | HU-84, HU-88 |
| PATCH | `/api/resenas/{id}/moderar` | GESTIONAR_RESENAS | HU-86 |
| PATCH | `/api/resenas/{id}/ocultar` | GESTIONAR_RESENAS | HU-85 |
| PUT | `/api/resenas/{id}/respuesta` | GESTIONAR_RESENAS | HU-87 |
| DELETE | `/api/resenas/{id}` | GESTIONAR_RESENAS | HU-83 |

### Para los demás microservicios

| Método | Ruta | Uso |
|---|---|---|
| GET | `/api/inventario/{sku}` | Stock de un producto |
| GET | `/api/inventario/stock?skus=A,B` | Stock de varios productos |
| GET | `/api/inventario/{sku}/disponible?cantidad=` | ¿Alcanza el stock? (carrito) |
| POST | `/api/inventario/{sku}/descontar` | Venta confirmada (HU-63) |
| POST | `/api/inventario/{sku}/reponer` | Cancelación o devolución |
| GET | `/api/inventario/bajo-stock` | Productos con stock bajo (dashboard) |

## Dependencias con ms-ventas

Mientras ms-ventas no exista:

- **Eliminar producto** responde 503: no puede comprobar si el producto tiene pedidos (HU-04).
- **Crear reseña** responde 503: no puede comprobar que el cliente lo haya recibido (HU-81).

Para probar la moderación de reseñas antes de tener ms-ventas, inserta una reseña de prueba en MySQL:

```sql
USE chibson_productos;
INSERT INTO resena (rut_cliente, nombre_cliente, calificacion, titulo, comentario, estado, fecha, sku)
VALUES ('9876543-3', 'Juan Pérez', 5, 'Excelente sonido', 'Muy buena terminación.', 'PENDIENTE', NOW(), 'GTR-0001');
```
