package chibson.usuarios.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Llamadas REST a ms-ventas. */
@Component
public class VentasClient {

    private final RestTemplate restTemplate;
    private final String urlVentas;

    public VentasClient(RestTemplate restTemplate, @Value("${servicios.ventas.url}") String urlVentas) {
        this.restTemplate = restTemplate;
        this.urlVentas = urlVentas;
    }

    /**
     * Pregunta a ms-ventas si el cliente tiene pedidos (HU-17).
     * Si ms-ventas no responde, no se puede asegurar que no los tenga,
     * así que se rechaza la operación en vez de arriesgarse a borrar un cliente con pedidos.
     */
    public boolean clienteTienePedidos(String rut) {
        try {
            Boolean respuesta = restTemplate.getForObject(
                    urlVentas + "/api/pedidos/cliente/" + rut + "/existe", Boolean.class);
            return Boolean.TRUE.equals(respuesta);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo verificar si el usuario tiene pedidos. Intente más tarde");
        }
    }
}
