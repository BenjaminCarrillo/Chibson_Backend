package chibson.productos.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Llamadas REST a ms-ventas.
 * Si ms-ventas no responde, se rechaza la operación con 503: no se puede borrar un producto
 * ni aceptar una reseña sin saber si hay pedidos de por medio.
 */
@Component
public class VentasClient {

    private final RestTemplate restTemplate;
    private final String urlVentas;

    public VentasClient(RestTemplate restTemplate, @Value("${servicios.ventas.url}") String urlVentas) {
        this.restTemplate = restTemplate;
        this.urlVentas = urlVentas;
    }

    /** ¿El producto aparece en algún pedido? (HU-04) */
    public boolean productoTienePedidos(String sku) {
        return consultar(urlVentas + "/api/pedidos/producto/{sku}/existe", sku);
    }

    /** ¿El cliente compró este producto y el pedido ya fue entregado? (HU-81) */
    public boolean clienteRecibioProducto(String rut, String sku) {
        return consultar(urlVentas + "/api/pedidos/cliente/{rut}/recibio/{sku}", rut, sku);
    }

    private boolean consultar(String url, Object... variables) {
        try {
            return Boolean.TRUE.equals(restTemplate.getForObject(url, Boolean.class, variables));
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar los pedidos en ms-ventas. Intente más tarde");
        }
    }
}
