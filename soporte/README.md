# ms-soporte

Microservicio del dominio **Soporte** de Chibson Instruments: tickets de los clientes,
respuestas del administrador y cierre con calificación (EP4, HU-26 a HU-32).

Puerto **8084**, base de datos MySQL **chibson_soporte**.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_soporte` se crea sola.
2. Revisar usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Levantar **ms-usuarios** (8081). Soporte lo usa para validar al cliente, revisar permisos,
   mostrar el nombre del cliente, enviar notificaciones y registrar el historial de acciones.
4. Ejecutar `SoporteApplication`.
5. Importar `chibson-soporte.postman_collection.json` en Postman y ejecutar las carpetas en orden.
   La carpeta 0 registra un cliente de prueba, y los números de ticket se guardan solos en variables.

## Estructura

```
chibson.soporte
├── model        Ticket, MensajeTicket, CategoriaTicket, EstadoTicket
├── repository   TicketRepository, MensajeTicketRepository
├── service      TicketService, Permisos
├── controller   TicketController
├── dto          TicketDTO, TicketRespuestaDTO, RespuestaDTO, EstadoDTO, CierreDTO,
│                UsuarioDTO, NotificacionDTO, RegistroAccionDTO, DetalleCambioDTO
├── client       UsuariosClient
└── config       RestTemplateConfig
```

## Flujo de un ticket

1. El cliente crea el ticket con asunto, categoría (DESPACHO, GARANTIA, PAGO, PEDIDO u OTRO),
   descripción y, si quiere, un adjunto. Queda **ABIERTO** y recibe el número de ticket.
2. El administrador responde (HU-29) y puede cambiar el estado en la misma acción, o solo cambiar
   el estado (HU-31). En ambos casos el cliente recibe una notificación.
3. Cuando el ticket está **RESUELTO**, el cliente lo cierra y puede calificar la atención de 1 a 5 (HU-30).
4. **CERRADO** es el estado final: ya no se puede responder ni cambiar.

## Reglas principales

- El administrador puede mover el ticket a cualquier estado distinto del actual. Si lo devuelve
  a ABIERTO o EN_PROCESO, se borra la fecha de resolución.
- Un cliente solo ve sus propios tickets; los de otro responden 404.
- El listado del administrador va del ticket más antiguo al más reciente (HU-28). El del cliente,
  del más reciente al más antiguo.
- Eliminar (HU-32) borra el ticket con su conversación. La confirmación la pide el frontend.
- Responder, cambiar el estado y eliminar quedan en el historial de acciones de ms-usuarios (HU-73).
- **Seguridad:** el RUT llega en el encabezado `X-Rut-Usuario`. Las acciones de administración
  necesitan el permiso `GESTIONAR_SOPORTE`.
- **Si ms-usuarios no responde:** se responde 503 cuando hace falta su respuesta (validar al
  cliente o el permiso). Las notificaciones y el historial no detienen la operación, y en los
  listados el nombre del cliente queda vacío.

## Endpoints

| Método | Ruta | Quién | HU |
|---|---|---|---|
| POST | `/api/tickets` `{asunto, categoria, descripcion, adjuntoUrl}` | Cliente | 26 |
| GET | `/api/tickets/mis-tickets` y `/mis-tickets/{numero}` | Cliente | 27 |
| PATCH | `/api/tickets/mis-tickets/{numero}/cerrar` `{calificacion, comentario}` | Cliente | 30 |
| GET | `/api/tickets?estado=` | GESTIONAR_SOPORTE | 28 |
| GET | `/api/tickets/{numero}` | GESTIONAR_SOPORTE | 28 |
| POST | `/api/tickets/{numero}/respuestas` `{contenido, estado}` | GESTIONAR_SOPORTE | 29 |
| PATCH | `/api/tickets/{numero}/estado` `{estado}` | GESTIONAR_SOPORTE | 31 |
| DELETE | `/api/tickets/{numero}` | GESTIONAR_SOPORTE | 32 |
| GET | `/api/tickets/abiertos/contar` | ms-informes (dashboard) | — |

## Supuestos y pendientes

- **Cierre automático (HU-30 criterio 3):** no se implementa porque necesita una tarea programada
  (`@Scheduled`), que quedó fuera del alcance. El criterio dice "puede", así que es opcional.
- **Adjunto:** se guarda como URL o ruta de la imagen, igual que las imágenes de productos y noticias.
- **Respuestas del cliente:** la plantilla solo pide que el administrador responda. `MensajeTicket`
  tiene `esAdministrador` por si más adelante se permite que el cliente también escriba.
- **"Tickets abiertos" del dashboard:** cuenta los ABIERTO y EN_PROCESO, es decir, los que todavía no se resuelven.
- **`TicketRespuestaDTO`:** DTO de salida que no está en el diagrama. Agrega el nombre del cliente
  que pide HU-28.
