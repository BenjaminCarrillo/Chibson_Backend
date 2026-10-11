package chibson.contenido.client;

import chibson.contenido.dto.RegistroAccionDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Llamadas REST a ms-usuarios.
 * Si ms-usuarios no responde, la acción del administrador no se cancela:
 * solo se deja una advertencia en el log.
 */
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

    public void registrarAccion(RegistroAccionDTO accion) {
        try {
            restTemplate.postForObject(urlUsuarios + "/api/acciones", accion, Void.class);
        } catch (RestClientException e) {
            log.warn("No se pudo registrar la acción en ms-usuarios: {}", e.getMessage());
        }
    }
}
