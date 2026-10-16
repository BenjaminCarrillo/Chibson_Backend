# ms-productos

Microservicio del dominio **Productos** de Chibson Instruments, con tres módulos:

- **Catálogo** (EP1): productos, categoría, SKU y ficha técnica. HU-01 a HU-07 y HU-19.
- **Stock** (EP7): stock y stock mínimo como campos de `Producto`; cada cambio queda en el historial de acciones. HU-63.
- **Reseñas** (EP11): calificaciones, moderación y respuestas. HU-81 a HU-88.

Puerto **8087**, base de datos MySQL **chibson_productos**.

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
│                Resena, EstadoResena                        (reseñas)
├── repository   uno por entidad
├── service      ProductoService, StockService, ResenaService, FichaTecnica
├── controller   ProductoController, StockController, ResenaController
├── dto          ProductoDTO, ProductoRespuestaDTO, StockDTO, MovimientoDTO, ResenaDTO, ...
├── client       UsuariosClient (permisos, nombre del cliente, historial), VentasClient (pedidos)
└── config       RestTemplateConfig
```

Como la tienda tiene una sola sucursal, no hay entidad de inventario: el stock es parte del producto.

## Reglas principales

- **SKU automático:** prefijo de la categoría y correlativo, por ejemplo `GTR-0001`, `TEC-0001`.
- **Stock:** campos `stockDisponible` y `stockMinimo` de `Producto`. "Agotado" es stock 0; "bajo stock" es stock igual o menor al mínimo (5 si no se indica).
- **Registro de cambios (historial, HU-73):** cada cambio de stock se envía al historial de acciones de ms-usuarios (módulo `Stock`), con el valor anterior y el nuevo, el pedido y quién lo provocó: el cliente al comprar, el administrador al cancelar o aprobar una devolución. Las ediciones manuales quedan en la acción "Modificó el producto". Para ver el registro de un producto: `GET /api/acciones?texto=GTR-0001` en ms-usuarios.
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
| GET | `/api/productos/{sku}/stock` | Stock de un producto |
| POST | `/api/productos/{sku}/descontar` `{cantidad, numeroPedido, rutResponsable}` | Venta confirmada (HU-63) |
| POST | `/api/productos/{sku}/reponer` `{cantidad, numeroPedido, tipo, rutResponsable}` | Cancelación o devolución |
| GET | `/api/productos/bajo-stock` | Productos con stock bajo (reportes) |

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
