# API Gateway

Puerta de entrada única del backend de Chibson Instruments. El frontend solo llama a
`http://localhost:8080`, y el gateway reenvía cada petición al microservicio que corresponde
según el comienzo de la ruta.

Está hecho con **Spring Cloud Gateway** (versión 2025.1.2, compatible con Spring Boot 4.1).
No tiene código propio: las rutas y CORS se configuran en `src/main/resources/application.yml`.

## Cómo ejecutarlo

1. Levantar los microservicios que se vayan a usar (8081 a 8086).
2. Ejecutar `GatewayApplication`.
3. Importar `chibson-gateway.postman_collection.json` y probar.

No necesita base de datos.

## Rutas

| Ruta | Microservicio |
|---|---|
| `/api/auth`, `/api/usuarios`, `/api/roles`, `/api/notificaciones`, `/api/acciones` | ms-usuarios (8081) |
| `/api/productos`, `/api/inventario`, `/api/resenas` | ms-productos (8082) |
| `/api/carrito`, `/api/pedidos`, `/api/devoluciones`, `/api/ventas` | ms-ventas (8083) |
| `/api/tickets` | ms-soporte (8084) |
| `/api/noticias`, `/api/institucional` | ms-contenido (8085) |
| `/api/dashboard`, `/api/informes` | ms-informes (8086) |

La ruta se reenvía tal cual. Por ejemplo, `GET http://localhost:8080/api/productos/GTR-0001`
llega a ms-productos como `GET http://localhost:8082/api/productos/GTR-0001`, con los mismos
encabezados (incluido `X-Rut-Usuario`) y el mismo cuerpo.

Si un microservicio está apagado, el gateway responde **503**. Una ruta que no está en la tabla responde **404**.

## CORS

El navegador solo deja que una página llame a otro origen (otro puerto cuenta como otro origen)
si el servidor lo permite. Está configurado así:

- **Orígenes:** `http://localhost:5500` y `http://127.0.0.1:5500` (Live Server de VS Code).
- **Métodos:** GET, POST, PUT, PATCH, DELETE y OPTIONS.
- **Encabezados:** `Content-Type` y `X-Rut-Usuario`.

Si el frontend se abre en otro puerto, hay que agregarlo en `allowedOrigins`. Los microservicios no
necesitan configurar CORS, porque el navegador nunca les habla directamente.

Postman no aplica CORS, así que para comprobarlo la colección envía un `OPTIONS` con el encabezado
`Origin`, como hace el navegador antes de un `fetch`.

## Uso desde el frontend

```js
const API = "http://localhost:8080";

// Pública
const productos = await fetch(`${API}/api/productos`).then(r => r.json());

// Con sesión: el RUT que devolvió el login
const carrito = await fetch(`${API}/api/carrito`, {
  headers: { "X-Rut-Usuario": sessionStorage.getItem("rut") }
}).then(r => r.json());
```

## Si no compila

La versión de Spring Cloud debe ser compatible con la de Spring Boot. Si Maven no encuentra
`spring-cloud-starter-gateway-server-webflux` o Spring avisa de incompatibilidad, generar un proyecto
nuevo en start.spring.io con la dependencia **Reactive Gateway** y copiar el `application.yml`.
