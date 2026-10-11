package chibson.ventas.service;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.dto.ItemDTO;
import chibson.ventas.dto.ProductoDTO;
import chibson.ventas.model.Carrito;
import chibson.ventas.model.ItemCarrito;
import chibson.ventas.client.ProductosClient;
import chibson.ventas.client.UsuariosClient;
import chibson.ventas.repository.CarritoRepository;

/** Carro de compras (HU-20 a HU-22). */
@Service
public class CarritoService {

    private final CarritoRepository carritoRepository;
    private final ProductosClient productosClient;
    private final UsuariosClient usuariosClient;

    public CarritoService(CarritoRepository carritoRepository, ProductosClient productosClient,
                          UsuariosClient usuariosClient) {
        this.carritoRepository = carritoRepository;
        this.productosClient = productosClient;
        this.usuariosClient = usuariosClient;
    }

    /** HU-20 y HU-21: carro del cliente con sesión (el RUT llega en el encabezado X-Rut-Usuario). */
    @Transactional
    public Carrito verCarrito(String rutHeader) {
        return obtener(rutCliente(rutHeader));
    }

    /** Carro del cliente (RUT ya validado); si todavía no tiene uno, se crea vacío. */
    @Transactional
    public Carrito obtener(String rutCliente) {
        return carritoRepository.findByRutCliente(rutCliente).orElseGet(() -> {
            Carrito nuevo = new Carrito();
            nuevo.setRutCliente(rutCliente);
            nuevo.setFechaActualizacion(LocalDateTime.now());
            return carritoRepository.save(nuevo);
        });
    }

    /** HU-20: agrega un producto. Si ya estaba, suma la cantidad. No puede superar el stock. */
    @Transactional
    public Carrito agregarProducto(String rutHeader, ItemDTO datos) {
        if (datos.getSku() == null || datos.getSku().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El SKU es obligatorio");
        }
        int cantidad = datos.getCantidad() == null ? 1 : datos.getCantidad();
        validarCantidad(cantidad);

        String sku = datos.getSku().trim().toUpperCase();
        Carrito carrito = obtener(rutCliente(rutHeader));
        ProductoDTO producto = productosClient.obtenerProducto(sku);

        ItemCarrito item = carrito.getItems().stream()
                .filter(i -> i.getSku().equals(sku))
                .findFirst()
                .orElse(null);
        int cantidadFinal = cantidad + (item == null ? 0 : item.getCantidad());
        validarStock(producto, cantidadFinal);

        if (item == null) {
            item = new ItemCarrito();
            item.setSku(sku);
            item.setCarrito(carrito);
            carrito.getItems().add(item);
        }
        item.setCantidad(cantidadFinal);
        item.setNombreProducto(producto.getModelo());
        item.setPrecioUnitario(producto.getPrecio());
        return guardar(carrito);
    }

    /** HU-21: cambia la cantidad de un producto del carro. */
    @Transactional
    public Carrito modificarCantidad(String rutHeader, Long idItem, Integer cantidad) {
        if (cantidad == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad es obligatoria");
        }
        validarCantidad(cantidad);
        Carrito carrito = obtener(rutCliente(rutHeader));
        ItemCarrito item = buscarItem(carrito, idItem);

        ProductoDTO producto = productosClient.obtenerProducto(item.getSku());
        validarStock(producto, cantidad);
        item.setCantidad(cantidad);
        item.setPrecioUnitario(producto.getPrecio());
        return guardar(carrito);
    }

    /** HU-22: quita un producto del carro. */
    @Transactional
    public Carrito eliminarProducto(String rutHeader, Long idItem) {
        Carrito carrito = obtener(rutCliente(rutHeader));
        carrito.getItems().remove(buscarItem(carrito, idItem));
        return guardar(carrito);
    }

    /** Se llama después de un pago aprobado. */
    @Transactional
    public void vaciar(String rutCliente) {
        carritoRepository.findByRutCliente(rutCliente).ifPresent(carrito -> {
            carrito.getItems().clear();
            guardar(carrito);
        });
    }

    /** Valida que el cliente exista y esté activo; devuelve su RUT normalizado por ms-usuarios. */
    private String rutCliente(String rutHeader) {
        return usuariosClient.obtenerCliente(rutHeader).getRut();
    }

    private ItemCarrito buscarItem(Carrito carrito, Long idItem) {
        return carrito.getItems().stream()
                .filter(i -> i.getIdItem().equals(idItem))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "El producto no está en su carro"));
    }

    private void validarCantidad(int cantidad) {
        if (cantidad < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser al menos 1");
        }
    }

    private void validarStock(ProductoDTO producto, int cantidad) {
        int stock = producto.getStockDisponible() == null ? 0 : producto.getStockDisponible();
        if (stock == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto " + producto.getSku() + " está agotado");
        }
        if (cantidad > stock) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo quedan " + stock + " unidades de " + producto.getModelo());
        }
    }

    private Carrito guardar(Carrito carrito) {
        carrito.setFechaActualizacion(LocalDateTime.now());
        return carritoRepository.save(carrito);
    }
}
