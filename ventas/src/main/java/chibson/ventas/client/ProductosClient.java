package chibson.ventas.client;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.dto.InventarioDTO;
import chibson.ventas.dto.MovimientoDTO;
import chibson.ventas.dto.ProductoDTO;

/**
 * Llamadas REST a ms-productos: datos del producto y movimientos de stock.
 * Si ms-productos no responde, la operación se rechaza con 503: no se puede vender sin saber
 * el precio ni el stock.
 */
@Component
public class ProductosClient {

    private static final Pattern MENSAJE = Pattern.compile("\"message\"\\s*:\\s*\"([^\"]*)\"");

    private final RestTemplate restTemplate;
    private final String urlProductos;

    public ProductosClient(RestTemplate restTemplate, @Value("${servicios.productos.url}") String urlProductos) {
        this.restTemplate = restTemplate;
        this.urlProductos = urlProductos;
    }

    /** Producto activo con su precio y stock actual. 404 si no existe o está desactivado. */
    public ProductoDTO obtenerProducto(String sku) {
        try {
            return restTemplate.getForObject(urlProductos + "/api/productos/{sku}", ProductoDTO.class, sku);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto " + sku + " no está disponible");
        } catch (RestClientException e) {
            throw noDisponible();
        }
    }

    /** Descuenta stock por una venta. Si no alcanza, ms-productos responde 409 y se propaga su mensaje. */
    public InventarioDTO descontarStock(String sku, int cantidad, Long numeroPedido) {
        MovimientoDTO movimiento = new MovimientoDTO(cantidad, numeroPedido, null);
        return enviarMovimiento(urlProductos + "/api/inventario/{sku}/descontar", movimiento, sku);
    }

    /** Devuelve stock al inventario. tipo = DEVOLUCION o CANCELACION. */
    public InventarioDTO reponerStock(String sku, int cantidad, Long numeroPedido, String tipo) {
        MovimientoDTO movimiento = new MovimientoDTO(cantidad, numeroPedido, tipo);
        return enviarMovimiento(urlProductos + "/api/inventario/{sku}/reponer", movimiento, sku);
    }

    private InventarioDTO enviarMovimiento(String url, MovimientoDTO movimiento, String sku) {
        try {
            return restTemplate.postForObject(url, movimiento, InventarioDTO.class, sku);
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), extraerMensaje(e));
        } catch (RestClientException e) {
            throw noDisponible();
        }
    }

    /** Toma el campo "message" del error que devolvió ms-productos. */
    private String extraerMensaje(HttpClientErrorException e) {
        Matcher m = MENSAJE.matcher(e.getResponseBodyAsString());
        return m.find() ? m.group(1) : "ms-productos rechazó la operación";
    }

    private ResponseStatusException noDisponible() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar los productos. Intente más tarde");
    }
}
