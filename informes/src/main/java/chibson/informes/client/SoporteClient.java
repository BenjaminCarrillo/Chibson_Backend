package chibson.informes.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/** Llamadas REST a ms-soporte. */
@Component
public class SoporteClient {

    private static final Logger log = LoggerFactory.getLogger(SoporteClient.class);

    private final RestTemplate restTemplate;
    private final String urlSoporte;

    public SoporteClient(RestTemplate restTemplate, @Value("${servicios.soporte.url}") String urlSoporte) {
        this.restTemplate = restTemplate;
        this.urlSoporte = urlSoporte;
    }

    /** Tickets ABIERTO o EN_PROCESO (HU-50). null si ms-soporte no responde. */
    public Long contarTicketsAbiertos() {
        try {
            return restTemplate.getForObject(urlSoporte + "/api/tickets/abiertos/contar", Long.class);
        } catch (RestClientException e) {
            log.warn("No se pudo contar los tickets abiertos: {}", e.getMessage());
            return null;
        }
    }
}
