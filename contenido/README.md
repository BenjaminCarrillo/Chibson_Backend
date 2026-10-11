# ms-contenido

Microservicio del dominio **Contenido y noticias** (EP10, HU-74 a HU-80) de Chibson Instruments.
Puerto **8085**, base de datos MySQL **chibson_contenido**.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_contenido` se crea sola.
2. Revisar usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Ejecutar `ContenidoApplication` desde el IDE, o `./mvnw spring-boot:run`.
4. Importar `chibson-contenido.postman_collection.json` en Postman y probar en orden.

Los tests (`ContenidoApplicationTests`) usan H2 en memoria, así que no necesitan MySQL.

## Estructura

```
chibson.contenido
├── model        Noticia, InformacionInstitucional, CategoriaNoticia, EstadoNoticia
├── repository   NoticiaRepository, InformacionInstitucionalRepository
├── service      NoticiaService, InformacionInstitucionalService
├── controller   NoticiaController, InformacionInstitucionalController
├── dto          NoticiaDTO, RegistroAccionDTO, DetalleCambioDTO
├── client       UsuariosClient (envía el historial a ms-usuarios)
└── config       RestTemplateConfig
```

## Endpoints

Las acciones de administración exigen el encabezado `X-Rut-Usuario` con el RUT del administrador.

| Método | Ruta | Uso | HU |
|---|---|---|---|
| GET | `/api/noticias/publicadas` | Noticias activadas, de la más reciente a la más antigua | HU-77 |
| GET | `/api/noticias/publicadas/{id}` | Detalle público (404 si no está activada) | HU-77 |
| GET | `/api/noticias?categoria=&estado=&titulo=` | Listado del administrador con filtros opcionales | HU-80 |
| GET | `/api/noticias/{id}` | Detalle para el administrador | — |
| POST | `/api/noticias` | Crear: `"publicar": true` publica, `false` deja borrador | HU-76, HU-80 |
| PUT | `/api/noticias/{id}` | Editar título, contenido, imagen, categoría o fecha | HU-74 |
| PATCH | `/api/noticias/{id}/activar` | Publicar un borrador o reactivar una noticia | HU-79 |
| PATCH | `/api/noticias/{id}/desactivar` | Ocultar sin eliminar | HU-79 |
| DELETE | `/api/noticias/{id}` | Eliminar definitivamente | HU-75 |
| GET | `/api/institucional` | Información de "Nosotros" | HU-78 |
| PUT | `/api/institucional` | Crear o actualizar misión, visión y contacto | HU-78 |

Valores de `categoria`: `PROMOCION`, `EVENTO`, `ANUNCIO`.
Valores de `estado`: `BORRADOR`, `ACTIVADA`, `DESACTIVADA`.

## Historial de acciones

Cada creación, modificación, activación, desactivación o eliminación envía un `RegistroAccionDTO`
a ms-usuarios (`POST http://localhost:8081/api/acciones`). Mientras ms-usuarios no exista, la
acción se guarda igual y en la consola aparece la advertencia
"No se pudo registrar la acción en ms-usuarios".

## Pendiente para más adelante

- El permiso de administrador todavía no se valida: solo se exige el RUT. Se completará cuando
  exista ms-usuarios (HU-72).
- CORS: cuando el frontend llame a través del API Gateway, se configura en el gateway.
