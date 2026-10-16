# ms-usuarios

Microservicio de los dominios **Registro de usuarios** (EP2) y **Roles y permisos** (EP9) de Chibson Instruments.
Puerto **8081**, base de datos MySQL **chibson_usuarios**.

## Cómo ejecutarlo

1. Tener MySQL encendido. La base `chibson_usuarios` se crea sola.
2. Revisar usuario y contraseña de MySQL en `src/main/resources/application.properties`.
3. Ejecutar `UsuariosApplication`.
4. Importar `chibson-usuarios.postman_collection.json` en Postman y probar en orden.

Al arrancar por primera vez se crean los 9 permisos, los roles `ADMINISTRADOR` (con todos los permisos)
y `CLIENTE` (sin permisos), y un administrador para entrar al panel:

| RUT | Correo | Contraseña |
|---|---|---|
| 12345678-5 | admin@chibson.cl | Admin1234 |

## Cómo se identifica al usuario

No hay JWT. El login devuelve el RUT, el rol y los permisos; el frontend los guarda mientras dura la sesión
y envía el RUT en el encabezado **`X-Rut-Usuario`** en cada petición. Las rutas de administración revisan
que ese usuario exista, esté activo y tenga el permiso necesario (HU-72):

- Sin encabezado o usuario inexistente/desactivado: **401** "Debe iniciar sesión".
- Usuario sin el permiso: **403** "No tiene permiso para realizar esta acción".

## Endpoints

| Método | Ruta | Quién | HU |
|---|---|---|---|
| POST | `/api/auth/registro` | Visitante | HU-08 |
| POST | `/api/auth/login` | Visitante | HU-09 |
| POST | `/api/auth/recuperar` `{rut, correo, nuevaPassword, confirmarPassword}` | Visitante | HU-10 |
| PUT | `/api/auth/password` | Usuario con sesión | HU-16 |
| GET / PUT | `/api/usuarios/perfil` | Usuario con sesión | HU-12 |
| GET | `/api/usuarios?rol=&estado=&texto=` | GESTIONAR_USUARIOS | HU-70 |
| GET | `/api/usuarios/{rut}` | GESTIONAR_USUARIOS | — |
| POST | `/api/usuarios` | GESTIONAR_USUARIOS | HU-13 |
| PUT | `/api/usuarios/{rut}` | GESTIONAR_USUARIOS | HU-14 |
| PATCH | `/api/usuarios/{rut}/rol` | GESTIONAR_USUARIOS | HU-70 |
| PATCH | `/api/usuarios/{rut}/desactivar` y `/reactivar` | GESTIONAR_USUARIOS | HU-15 |
| DELETE | `/api/usuarios/{rut}` | GESTIONAR_USUARIOS | HU-17 |
| GET | `/api/roles` y `/api/roles/permisos` | GESTIONAR_USUARIOS | HU-71 |
| PUT | `/api/roles/{idRol}/permisos` | GESTIONAR_USUARIOS | HU-71 |
| GET | `/api/acciones?texto=&tipo=&administrador=&desde=&hasta=` | VER_HISTORIAL | HU-73 |
| GET | `/api/acciones/{id}` | VER_HISTORIAL | HU-73 |
| GET | `/api/acciones/administrador/{rut}` | VER_HISTORIAL | HU-73 |
| GET | `/api/notificaciones` | Usuario con sesión | — |
| PATCH | `/api/notificaciones/{id}/leida` | Usuario con sesión | — |

### Para los demás microservicios

| Método | Ruta | Uso |
|---|---|---|
| POST | `/api/acciones` | Registrar una acción de un administrador (lo usa ms-contenido) |
| POST | `/api/notificaciones` | Avisarle algo a un usuario |
| GET | `/api/roles/verificar?rut=&permiso=` | Saber si un usuario tiene un permiso |
| GET | `/api/usuarios/{rut}/datos` | Nombre y dirección del perfil (ventas, soporte) |
| GET | `/api/usuarios/registrados-hoy` | Clientes nuevos del día (dashboard) |

## Decisiones dentro del alcance

- **Contraseñas sin hash:** BCrypt queda fuera porque no se ha visto en clases. Igual nunca se devuelven: las respuestas usan `UsuarioDTO`.
- **Recuperación de contraseña (HU-10):** sin enlace ni token. El usuario envía su RUT, su correo y la contraseña nueva; `Usuario.recuperarPassword()` la cambia solo si el correo es el de esa cuenta. Las contraseñas se guardan sin encriptar.
- **Cambio de correo:** el perfil no permite cambiar el correo, porque HU-12 exige verificarlo y eso necesita envío de correos. El administrador sí puede cambiarlo (HU-14).
- **Eliminar usuario (HU-17):** se consulta a ms-ventas si el cliente tiene pedidos. Si ms-ventas no responde, se rechaza con 503 para no borrar a alguien con pedidos.
- **Cerrar sesión (HU-18):** es solo del frontend: borra los datos guardados de la sesión.
