package chibson.informes.client;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import chibson.informes.dto.PedidoDTO;

/** Llamadas REST a ms-ventas: pedidos y devoluciones. */
@Component
public class VentasClient {

    private static final Logger log = LoggerFactory.getLogger(VentasClient.class);

    private final RestTemplate restTemplate;
    private final String urlVentas;

    public VentasClient(RestTemplate restTemplate, @Value("${servicios.ventas.url}") String urlVentas) {
        this.restTemplate = restTemplate;
        this.urlVentas = urlVentas;
    }

    /**
     * Pedidos creados entre las fechas (null = sin límite). Sin estos datos no hay informe,
     * así que si ms-ventas no responde se lanza 503.
     */
    public List<PedidoDTO> obtenerPedidos(LocalDate desde, LocalDate hasta) {
        try {
            PedidoDTO[] pedidos = restTemplate.getForObject(
                    urlVentas + "/api/pedidos/resumen?desde={desde}&hasta={hasta}", PedidoDTO[].class,
                    desde == null ? "" : desde.toString(), hasta == null ? "" : hasta.toString());
            return pedidos == null ? List.of() : Arrays.asList(pedidos);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar las ventas. Intente más tarde");
        }
    }

    /** Devoluciones pendientes (HU-52). null si ms-ventas no responde. */
    public Long contarDevolucionesPendientes() {
        try {
            return restTemplate.getForObject(urlVentas + "/api/devoluciones/pendientes/contar", Long.class);
        } catch (RestClientException e) {
            log.warn("No se pudo contar las devoluciones pendientes: {}", e.getMessage());
            return null;
        }
    }
}
