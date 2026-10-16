package chibson.contenido.client;

import chibson.contenido.dto.RegistroAccionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Llamadas REST a ms-usuarios: permisos (HU-72) e historial de acciones (HU-73). */
@Component
public class UsuariosClient {

    private static final Logger log = LoggerFactory.getLogger(UsuariosClient.class);

    private final RestTemplate restTemplate;
    private final String urlUsuarios;

    public UsuariosClient(RestTemplate restTemplate,
                          @Value("${servicios.usuarios.url}") String urlUsuarios) {
        this.restTemplate = restTemplate;
        this.urlUsuarios = urlUsuarios;
    }

    /**
     * Corta la petición si el usuario no tiene el permiso (HU-72).
     * 401 si no hay sesión, 403 si no tiene el permiso.
     * Si ms-usuarios no responde, no se puede comprobar el permiso y se rechaza con 503.
     */
    public void exigirPermiso(String rut, String permiso) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        Boolean permitido;
        try {
            permitido = restTemplate.getForObject(
                    urlUsuarios + "/api/roles/verificar?rut={rut}&permiso={permiso}",
                    Boolean.class, rut.trim(), permiso);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar el permiso. Intente más tarde");
        }
        if (!Boolean.TRUE.equals(permitido)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción");
        }
    }

    /**
     * Envía una acción al historial.
     * Si ms-usuarios no responde, la acción del administrador no se cancela:
     * solo se deja una advertencia en el log.
     */
    public void registrarAccion(RegistroAccionDTO accion) {
        try {
            restTemplate.postForObject(urlUsuarios + "/api/acciones", accion, Void.class);
        } catch (RestClientException e) {
            log.warn("No se pudo registrar la acción en ms-usuarios: {}", e.getMessage());
        }
    }
}
