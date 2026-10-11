package chibson.informes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Llamadas REST a ms-usuarios: permisos y clientes nuevos. */
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

    /** Usuarios registrados hoy (HU-52). null si ms-usuarios no responde. */
    public Long contarRegistradosHoy() {
        try {
            return restTemplate.getForObject(urlUsuarios + "/api/usuarios/registrados-hoy", Long.class);
        } catch (RestClientException e) {
            log.warn("No se pudo contar los usuarios registrados hoy: {}", e.getMessage());
            return null;
        }
    }
}
