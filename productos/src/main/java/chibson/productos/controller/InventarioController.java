package chibson.productos.controller;

import chibson.productos.client.UsuariosClient;
import chibson.productos.dto.InventarioDTO;
import chibson.productos.dto.MovimientoDTO;
import chibson.productos.dto.ProductoRespuestaDTO;
import chibson.productos.model.MovimientoStock;
import chibson.productos.service.InventarioService;
import chibson.productos.service.ProductoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventario")
public class InventarioController {

    private final InventarioService inventarioService;
    private final ProductoService productoService;
    private final UsuariosClient usuariosClient;

    public InventarioController(InventarioService inventarioService, ProductoService productoService,
                                UsuariosClient usuariosClient) {
        this.inventarioService = inventarioService;
        this.productoService = productoService;
        this.usuariosClient = usuariosClient;
    }

    // ---------- Para otros microservicios ----------

    @GetMapping("/{sku}")
    public InventarioDTO obtenerPorSku(@PathVariable String sku) {
        return inventarioService.aDTO(inventarioService.obtenerPorSku(sku));
    }

    /** Stock de varios productos: /api/inventario/stock?skus=GTR-0001,TEC-0001 */
    @GetMapping("/stock")
    public List<InventarioDTO> obtenerStockPorSkus(@RequestParam List<String> skus) {
        return inventarioService.obtenerStockPorSkus(skus);
    }

    /** Lo usa el carrito antes de agregar o aumentar unidades (HU-20, HU-21). */
    @GetMapping("/{sku}/disponible")
    public boolean verificarDisponibilidad(@PathVariable String sku, @RequestParam int cantidad) {
        return inventarioService.verificarDisponibilidad(sku, cantidad);
    }

    /** Lo usa ms-ventas al confirmarse el pago (HU-63). */
    @PostMapping("/{sku}/descontar")
    public InventarioDTO descontarStock(@PathVariable String sku, @RequestBody MovimientoDTO datos) {
        return inventarioService.descontarStock(sku, datos);
    }

    /** Lo usa ms-ventas al cancelar un pedido o aprobar una devolución. */
    @PostMapping("/{sku}/reponer")
    public InventarioDTO reponerStock(@PathVariable String sku, @RequestBody MovimientoDTO datos) {
        return inventarioService.reponerStock(sku, datos);
    }

    /** Productos con stock bajo; lo usa el dashboard (HU-50, HU-56). */
    @GetMapping("/bajo-stock")
    public List<ProductoRespuestaDTO> listarBajoStock() {
        return productoService.listarBajoStock();
    }

    // ---------- Panel de administración ----------

    /** Historial de entradas y salidas del producto (HU-64). */
    @GetMapping("/{sku}/movimientos")
    public List<MovimientoStock> listarMovimientos(@RequestHeader(value = "X-Rut-Usuario", required = false) String rut,
                                                   @PathVariable String sku) {
        usuariosClient.exigirPermiso(rut, "GESTIONAR_PRODUCTOS");
        return inventarioService.listarMovimientos(sku);
    }
}
