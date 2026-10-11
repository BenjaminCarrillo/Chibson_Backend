package chibson.informes.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import chibson.informes.dto.ProductoVendidoDTO;
import chibson.informes.dto.PuntoGraficoDTO;
import chibson.informes.dto.StockBajoDTO;
import chibson.informes.model.Dashboard;
import chibson.informes.service.DashboardService;

/** Dashboard de administración, permiso VER_INFORMES (HU-50 a HU-56). */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final String RUT = "X-Rut-Usuario";

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** HU-50 y HU-52 */
    @GetMapping("/kpis")
    public Dashboard obtenerKpis(@RequestHeader(value = RUT, required = false) String rut) {
        return dashboardService.obtenerKpis(rut);
    }

    /** HU-51 */
    @GetMapping("/ventas-7-dias")
    public List<PuntoGraficoDTO> ventasUltimos7Dias(@RequestHeader(value = RUT, required = false) String rut) {
        return dashboardService.ventasUltimos7Dias(rut);
    }

    /** HU-53 */
    @GetMapping("/ventas-categoria")
    public List<PuntoGraficoDTO> ventasPorCategoria(@RequestHeader(value = RUT, required = false) String rut) {
        return dashboardService.ventasPorCategoria(rut);
    }

    /** HU-54 */
    @GetMapping("/top-productos")
    public List<ProductoVendidoDTO> topProductos(@RequestHeader(value = RUT, required = false) String rut) {
        return dashboardService.topProductos(rut);
    }

    /** HU-55 */
    @GetMapping("/pedidos-region")
    public List<PuntoGraficoDTO> pedidosPorRegion(@RequestHeader(value = RUT, required = false) String rut) {
        return dashboardService.pedidosPorRegion(rut);
    }

    /** HU-56 */
    @GetMapping("/stock-bajo")
    public List<StockBajoDTO> productosStockBajo(@RequestHeader(value = RUT, required = false) String rut) {
        return dashboardService.productosStockBajo(rut);
    }
}
