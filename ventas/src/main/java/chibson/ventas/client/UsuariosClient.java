package chibson.ventas.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.dto.NotificacionDTO;
import chibson.ventas.dto.RegistroAccionDTO;
import chibson.ventas.dto.UsuarioDTO;

/**
 * Llamadas REST a ms-usuarios: permisos, datos del cliente, notificaciones e historial de acciones.
 * Las notificaciones y el historial son tolerantes: si fallan solo se deja un aviso en el log.
 */
@Component
public class UsuariosClient {

    private static final Logger log = LoggerFactory.getLogger(UsuariosClient.class);

    private final RestTemplate restTemplate;
    private final String urlUsuarios;

    public UsuariosClient(RestTemplate restTemplate, @Value("${servicios.usuarios.url}") String urlUsuarios) {
        this.restTemplate = restTemplate;
        this.urlUsuarios = urlUsuarios;
    }

    /** Lanza 401 sin sesión, 403 sin permiso y 503 si ms-usuarios no responde. */
    public void exigirPermiso(String rut, String permiso) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        Boolean permitido;
        try {
            permitido = restTemplate.getForObject(
                    urlUsuarios + "/api/roles/verificar?rut={rut}&permiso={permiso}", Boolean.class, rut.trim(), permiso);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar el permiso. Intente más tarde");
        }
        if (!Boolean.TRUE.equals(permitido)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción");
        }
    }

    /**
     * Cliente con sesión: debe existir y estar activo. Devuelve sus datos (nombre y dirección del perfil).
     * 401 si no hay sesión o la cuenta no es válida, 503 si ms-usuarios no responde.
     */
    public UsuarioDTO obtenerCliente(String rut) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        UsuarioDTO usuario;
        try {
            usuario = restTemplate.getForObject(urlUsuarios + "/api/usuarios/{rut}/datos", UsuarioDTO.class, rut.trim());
        } catch (HttpClientErrorException e) {
            // 404 (no existe) o 400 (RUT mal escrito): el encabezado no corresponde a un usuario válido
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo validar el usuario. Intente más tarde");
        }
        if (usuario == null || "DESACTIVADO".equals(usuario.getEstado())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La cuenta no está activa");
        }
        return usuario;
    }

    /** Para listados: si no se encuentra el usuario o ms-usuarios no responde, devuelve null. */
    public UsuarioDTO buscarUsuario(String rut) {
        try {
            return restTemplate.getForObject(urlUsuarios + "/api/usuarios/{rut}/datos", UsuarioDTO.class, rut);
        } catch (RestClientException e) {
            log.warn("No se pudieron obtener los datos del usuario {}: {}", rut, e.getMessage());
            return null;
        }
    }

    public void enviarNotificacion(String rutUsuario, String mensaje, String referencia) {
        try {
            restTemplate.postForObject(urlUsuarios + "/api/notificaciones",
                    new NotificacionDTO(rutUsuario, mensaje, referencia), Void.class);
        } catch (RestClientException e) {
            log.warn("No se pudo enviar la notificación a {}: {}", rutUsuario, e.getMessage());
        }
    }

    public void registrarAccion(RegistroAccionDTO accion) {
        try {
            restTemplate.postForObject(urlUsuarios + "/api/acciones", accion, Void.class);
        } catch (RestClientException e) {
            log.warn("No se pudo registrar la acción en ms-usuarios: {}", e.getMessage());
        }
    }
}
