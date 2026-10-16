package chibson.productos.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chibson.productos.dto.MovimientoDTO;
import chibson.productos.dto.ProductoRespuestaDTO;
import chibson.productos.dto.StockDTO;
import chibson.productos.service.ProductoService;
import chibson.productos.service.StockService;

/** Stock de los productos; lo usan ms-ventas y ms-informes (EP7). */
@RestController
@RequestMapping("/api/productos")
public class StockController {

    private final StockService stockService;
    private final ProductoService productoService;

    public StockController(StockService stockService, ProductoService productoService) {
        this.stockService = stockService;
        this.productoService = productoService;
    }

    @GetMapping("/{sku}/stock")
    public StockDTO consultar(@PathVariable String sku) {
        return stockService.consultar(sku);
    }

    /** ms-ventas al confirmarse el pago (HU-63). */
    @PostMapping("/{sku}/descontar")
    public StockDTO descontar(@PathVariable String sku, @RequestBody MovimientoDTO datos) {
        return stockService.descontar(sku, datos);
    }

    /** ms-ventas al cancelar un pedido o aprobar una devolución. */
    @PostMapping("/{sku}/reponer")
    public StockDTO reponer(@PathVariable String sku, @RequestBody MovimientoDTO datos) {
        return stockService.reponer(sku, datos);
    }

    /** Productos activos con stock bajo; lo usa ms-informes para el resumen general (HU-50). */
    @GetMapping("/bajo-stock")
    public List<ProductoRespuestaDTO> listarBajoStock() {
        return productoService.listarBajoStock();
    }
}
