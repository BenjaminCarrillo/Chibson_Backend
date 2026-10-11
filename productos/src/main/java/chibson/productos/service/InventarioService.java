package chibson.productos.service;

import chibson.productos.dto.InventarioDTO;
import chibson.productos.dto.MovimientoDTO;
import chibson.productos.model.EstadoProducto;
import chibson.productos.model.Inventario;
import chibson.productos.model.MovimientoStock;
import chibson.productos.model.Producto;
import chibson.productos.model.TipoMovimiento;
import chibson.productos.repository.InventarioRepository;
import chibson.productos.repository.MovimientoStockRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Módulo de inventario (EP7): stock, stock mínimo y movimientos. */
@Service
public class InventarioService {

    /** Stock mínimo por defecto si el administrador no indica uno (HU-01). */
    public static final int STOCK_MINIMO_POR_DEFECTO = 5;

    private final InventarioRepository inventarioRepository;
    private final MovimientoStockRepository movimientoRepository;

    public InventarioService(InventarioRepository inventarioRepository, MovimientoStockRepository movimientoRepository) {
        this.inventarioRepository = inventarioRepository;
        this.movimientoRepository = movimientoRepository;
    }

    /** Crea el inventario de un producto nuevo; el stock inicial queda como ajuste manual. */
    public Inventario crearInventario(Producto producto, Integer stockInicial, Integer stockMinimo) {
        validarNoNegativo(stockInicial, "stock inicial");
        int minimo = (stockMinimo == null) ? STOCK_MINIMO_POR_DEFECTO : stockMinimo;
        validarNoNegativo(minimo, "stock mínimo");

        Inventario inventario = new Inventario();
        inventario.setProducto(producto);
        inventario.setStockDisponible(stockInicial);
        inventario.setStockMinimo(minimo);
        inventario.setFechaActualizacion(LocalDateTime.now());
        Inventario guardado = inventarioRepository.save(inventario);
        producto.setInventario(guardado);
        if (stockInicial > 0) {
            registrarMovimiento(guardado, TipoMovimiento.AJUSTE_MANUAL, stockInicial, null);
        }
        return guardado;
    }

    public Inventario obtenerPorSku(String sku) {
        return inventarioRepository.findByProductoSku(sku)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe inventario para el SKU " + sku));
    }

    /**
     * El administrador fija el stock y el stock mínimo desde la edición del producto (HU-02).
     * Si cambia la cantidad, se registra un AJUSTE_MANUAL con la diferencia (HU-64).
     */
    public Inventario ajustarStock(String sku, Integer nuevoStock, Integer nuevoMinimo) {
        Inventario inventario = obtenerPorSku(sku);
        if (nuevoStock != null) {
            validarNoNegativo(nuevoStock, "stock disponible");
            int diferencia = nuevoStock - inventario.getStockDisponible();
            if (diferencia != 0) {
                inventario.setStockDisponible(nuevoStock);
                registrarMovimiento(inventario, TipoMovimiento.AJUSTE_MANUAL, diferencia, null);
            }
        }
        if (nuevoMinimo != null) {
            validarNoNegativo(nuevoMinimo, "stock mínimo");
            inventario.setStockMinimo(nuevoMinimo);
        }
        inventario.setFechaActualizacion(LocalDateTime.now());
        return inventarioRepository.save(inventario);
    }

    /** ¿Hay al menos esta cantidad disponible? Lo usa el carrito (HU-20, HU-21). */
    public boolean verificarDisponibilidad(String sku, int cantidad) {
        return inventarioRepository.findByProductoSku(sku)
                .map(i -> i.getProducto().getEstado() == EstadoProducto.ACTIVO && i.getStockDisponible() >= cantidad)
                .orElse(false);
    }

    /** Stock de varios SKU en una sola consulta (para los listados de ms-ventas). */
    public List<InventarioDTO> obtenerStockPorSkus(List<String> skus) {
        return inventarioRepository.findByProductoSkuIn(skus).stream().map(this::aDTO).toList();
    }

    /** Descuenta lo vendido al confirmarse el pago (HU-63). Rechaza si no alcanza el stock. */
    @Transactional
    public InventarioDTO descontarStock(String sku, MovimientoDTO datos) {
        validarCantidad(datos.getCantidad());
        Inventario inventario = obtenerPorSku(sku);
        if (inventario.getStockDisponible() < datos.getCantidad()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente para " + sku + ": quedan " + inventario.getStockDisponible() + " unidades");
        }
        inventario.setStockDisponible(inventario.getStockDisponible() - datos.getCantidad());
        inventario.setFechaActualizacion(LocalDateTime.now());
        inventarioRepository.save(inventario);
        registrarMovimiento(inventario, TipoMovimiento.VENTA, -datos.getCantidad(), datos.getNumeroPedido());
        return aDTO(inventario);
    }

    /** Devuelve unidades por una devolución aprobada (HU-41) o un pedido cancelado (HU-37). */
    @Transactional
    public InventarioDTO reponerStock(String sku, MovimientoDTO datos) {
        validarCantidad(datos.getCantidad());
        if (datos.getTipo() != TipoMovimiento.DEVOLUCION && datos.getTipo() != TipoMovimiento.CANCELACION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El tipo debe ser DEVOLUCION o CANCELACION");
        }
        Inventario inventario = obtenerPorSku(sku);
        inventario.setStockDisponible(inventario.getStockDisponible() + datos.getCantidad());
        inventario.setFechaActualizacion(LocalDateTime.now());
        inventarioRepository.save(inventario);
        registrarMovimiento(inventario, datos.getTipo(), datos.getCantidad(), datos.getNumeroPedido());
        return aDTO(inventario);
    }

    /** Productos activos con stock igual o menor a su mínimo (dashboard, HU-56). */
    public List<Inventario> listarBajoStock() {
        return inventarioRepository.findAll().stream()
                .filter(i -> i.getProducto().getEstado() == EstadoProducto.ACTIVO)
                .filter(Inventario::esBajoStock)
                .sorted((a, b) -> Integer.compare(a.getStockDisponible(), b.getStockDisponible()))
                .toList();
    }

    /** Historial de entradas y salidas de un producto, del más nuevo al más antiguo (HU-64). */
    public List<MovimientoStock> listarMovimientos(String sku) {
        obtenerPorSku(sku);
        return movimientoRepository.findByInventarioProductoSkuOrderByFechaDesc(sku);
    }

    public InventarioDTO aDTO(Inventario inventario) {
        return new InventarioDTO(inventario.getProducto().getSku(), inventario.getStockDisponible(),
                inventario.getStockMinimo(), inventario.esBajoStock());
    }

    private void registrarMovimiento(Inventario inventario, TipoMovimiento tipo, int cantidad, Long numeroPedido) {
        MovimientoStock movimiento = new MovimientoStock();
        movimiento.setInventario(inventario);
        movimiento.setTipo(tipo);
        movimiento.setCantidad(cantidad);
        movimiento.setSaldoResultante(inventario.getStockDisponible());
        movimiento.setNumeroPedido(numeroPedido);
        movimiento.setFecha(LocalDateTime.now());
        movimientoRepository.save(movimiento);
    }

    private void validarNoNegativo(Integer valor, String campo) {
        if (valor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El " + campo + " es obligatorio");
        }
        if (valor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El " + campo + " no puede ser negativo");
        }
    }

    private void validarCantidad(Integer cantidad) {
        if (cantidad == null || cantidad <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser mayor que cero");
        }
    }
}
