package chibson.productos.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.productos.client.UsuariosClient;
import chibson.productos.dto.DetalleCambioDTO;
import chibson.productos.dto.MovimientoDTO;
import chibson.productos.dto.RegistroAccionDTO;
import chibson.productos.dto.StockDTO;
import chibson.productos.model.Producto;
import chibson.productos.repository.ProductoRepository;

/**
 * Stock de los productos (EP7). Como la tienda tiene una sola sucursal, el stock es un campo de
 * Producto. Cada cambio se registra en el historial de acciones de ms-usuarios con el valor
 * anterior y el nuevo.
 */
@Service
public class StockService {

    /** Stock mínimo por defecto si el administrador no indica uno (HU-01). */
    public static final int STOCK_MINIMO_POR_DEFECTO = 5;
    private static final String MODULO = "Stock";

    private final ProductoRepository productoRepository;
    private final UsuariosClient usuariosClient;

    public StockService(ProductoRepository productoRepository, UsuariosClient usuariosClient) {
        this.productoRepository = productoRepository;
        this.usuariosClient = usuariosClient;
    }

    public StockDTO consultar(String sku) {
        return aDTO(buscar(sku));
    }

    /** Descuenta lo vendido al confirmarse el pago (HU-63). Rechaza si no alcanza el stock. */
    @Transactional
    public StockDTO descontar(String sku, MovimientoDTO datos) {
        validarCantidad(datos.getCantidad());
        Producto producto = buscar(sku);
        int anterior = producto.getStockDisponible();
        if (anterior < datos.getCantidad()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente para " + sku + ": quedan " + anterior + " unidades");
        }
        producto.setStockDisponible(anterior - datos.getCantidad());
        productoRepository.save(producto);
        registrar(datos, "Descontó " + datos.getCantidad() + " unidad(es) de " + producto.getModelo() + " (" + sku
                + ") por la venta del pedido #" + datos.getNumeroPedido(), anterior, producto.getStockDisponible());
        return aDTO(producto);
    }

    /** Devuelve unidades por una devolución aprobada (HU-41) o un pedido cancelado (HU-37). */
    @Transactional
    public StockDTO reponer(String sku, MovimientoDTO datos) {
        validarCantidad(datos.getCantidad());
        String motivo;
        if ("DEVOLUCION".equals(datos.getTipo())) {
            motivo = "la devolución";
        } else if ("CANCELACION".equals(datos.getTipo())) {
            motivo = "la cancelación";
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo debe ser DEVOLUCION o CANCELACION");
        }
        Producto producto = buscar(sku);
        int anterior = producto.getStockDisponible();
        producto.setStockDisponible(anterior + datos.getCantidad());
        productoRepository.save(producto);
        registrar(datos, "Repuso " + datos.getCantidad() + " unidad(es) de " + producto.getModelo() + " (" + sku
                + ") por " + motivo + " del pedido #" + datos.getNumeroPedido(), anterior, producto.getStockDisponible());
        return aDTO(producto);
    }

    public StockDTO aDTO(Producto p) {
        return new StockDTO(p.getSku(), p.getStockDisponible(), p.getStockMinimo(), p.esBajoStock());
    }

    private Producto buscar(String sku) {
        return productoRepository.findById(sku)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto " + sku));
    }

    private void registrar(MovimientoDTO datos, String descripcion, int anterior, int nuevo) {
        RegistroAccionDTO accion = new RegistroAccionDTO();
        accion.setRutAdministrador(datos.getRutResponsable() == null || datos.getRutResponsable().isBlank()
                ? "SISTEMA" : datos.getRutResponsable().trim());
        accion.setTipo("MODIFICAR");
        accion.setModulo(MODULO);
        accion.setDescripcion(descripcion);
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        cambios.add(new DetalleCambioDTO("stockDisponible", String.valueOf(anterior), String.valueOf(nuevo)));
        accion.setCambios(cambios);
        usuariosClient.registrarAccion(accion);
    }

    private void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero");
        }
    }
}
