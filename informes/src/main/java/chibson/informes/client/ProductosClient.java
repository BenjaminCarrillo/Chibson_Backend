package chibson.informes.client;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import chibson.informes.dto.ProductoDTO;

/** Llamadas REST a ms-productos. */
@Component
public class ProductosClient {

    private final RestTemplate restTemplate;
    private final String urlProductos;

    public ProductosClient(RestTemplate restTemplate, @Value("${servicios.productos.url}") String urlProductos) {
        this.restTemplate = restTemplate;
        this.urlProductos = urlProductos;
    }

    /** Productos activos con stock igual o menor a su mínimo. 503 si ms-productos no responde. */
    public List<ProductoDTO> obtenerStockBajo() {
        try {
            ProductoDTO[] productos = restTemplate.getForObject(urlProductos + "/api/inventario/bajo-stock",
                    ProductoDTO[].class);
            return productos == null ? List.of() : Arrays.asList(productos);
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo consultar el inventario. Intente más tarde");
        }
    }
}
