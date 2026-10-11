package chibson.productos.client;

import chibson.productos.dto.RegistroAccionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Llamadas REST a ms-usuarios: permisos, nombre del cliente e historial. */
@Component
public class UsuariosClient {

    private static final Logger log = LoggerFactory.getLogger(UsuariosClient.class);

    private final RestTemplate restTemplate;
    private final String urlUsuarios;

    public UsuariosClient(RestTemplate restTemplate, @Value("${servicios.usuarios.url}") String urlUsuarios) {
        this.restTemplate = restTemplate;
        this.urlUsuarios = urlUsuarios;
    }

    /** 401 sin sesión, 403 sin permiso, 503 si ms-usuarios no responde (HU-72). */
    public void exigirPermiso(String rut, String permiso) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        Boolean permitido;
        try {
            permitido = restTemplate.getForObject(urlUsuarios + "/api/roles/verificar?rut={rut}&permiso={permiso}",
                    Boolean.class, rut.trim(), permiso);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo verificar el permiso. Intente más tarde");
        }
        if (!Boolean.TRUE.equals(permitido)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción");
        }
    }

    /**
     * Nombre completo del cliente para mostrarlo en su reseña (HU-82).
     * Si el RUT no existe en ms-usuarios, se trata como si no hubiera sesión.
     */
    @SuppressWarnings("unchecked")
    public String obtenerNombre(String rut) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        try {
            Map<String, Object> datos = restTemplate.getForObject(urlUsuarios + "/api/usuarios/{rut}/datos",
                    Map.class, rut.trim());
            if (datos == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
            }
            return datos.get("nombres") + " " + datos.get("primerApellido");
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "No se pudo validar al usuario. Intente más tarde");
        }
    }

    /** Envía una acción al historial; si ms-usuarios no responde, solo se advierte en el log. */
    public void registrarAccion(RegistroAccionDTO accion) {
        try {
            restTemplate.postForObject(urlUsuarios + "/api/acciones", accion, Void.class);
        } catch (RestClientException e) {
            log.warn("No se pudo registrar la acción en ms-usuarios: {}", e.getMessage());
        }
    }
}
