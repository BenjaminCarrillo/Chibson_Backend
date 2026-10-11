package chibson.informes.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import chibson.informes.client.ProductosClient;
import chibson.informes.client.SoporteClient;
import chibson.informes.client.UsuariosClient;
import chibson.informes.client.VentasClient;
import chibson.informes.dto.DetalleDTO;
import chibson.informes.dto.PedidoDTO;
import chibson.informes.dto.ProductoDTO;
import chibson.informes.dto.ProductoVendidoDTO;
import chibson.informes.dto.PuntoGraficoDTO;
import chibson.informes.dto.StockBajoDTO;
import chibson.informes.model.Dashboard;

/**
 * Dashboard de administración (HU-50 a HU-56). Todo se calcula al momento de pedirlo,
 * así que siempre muestra datos actualizados.
 */
@Service
public class DashboardService {

    private final VentasClient ventasClient;
    private final ProductosClient productosClient;
    private final UsuariosClient usuariosClient;
    private final SoporteClient soporteClient;

    public DashboardService(VentasClient ventasClient, ProductosClient productosClient,
                            UsuariosClient usuariosClient, SoporteClient soporteClient) {
        this.ventasClient = ventasClient;
        this.productosClient = productosClient;
        this.usuariosClient = usuariosClient;
        this.soporteClient = soporteClient;
    }

    /** HU-50 y HU-52: tarjetas KPI y resumen rápido. */
    public Dashboard obtenerKpis(String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        LocalDate inicioMesAnterior = inicioMes.minusMonths(1);
        // Mismo rango de días del mes anterior: si hoy es 31 y el mes anterior tuvo 30, llega hasta el 30
        LocalDate finComparacion = inicioMesAnterior.withDayOfMonth(
                Math.min(hoy.getDayOfMonth(), inicioMesAnterior.lengthOfMonth()));

        List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(null, null);
        List<PedidoDTO> ventasHoy = Calculos.ventasEntre(pedidos, hoy, hoy);
        List<PedidoDTO> ventasMes = Calculos.ventasEntre(pedidos, inicioMes, hoy);
        int totalMes = Calculos.sumarTotales(ventasMes);
        int totalMesAnterior = Calculos.sumarTotales(Calculos.ventasEntre(pedidos, inicioMesAnterior, finComparacion));

        Dashboard d = new Dashboard();
        d.setVentasDia(Calculos.sumarTotales(ventasHoy));
        d.setCantidadVentasDia(ventasHoy.size());
        d.setVentasMes(totalMes);
        d.setVariacionMes(totalMesAnterior == 0 ? null
                : Math.round((totalMes - totalMesAnterior) * 1000.0 / totalMesAnterior) / 10.0);
        d.setPedidosPendientes(pedidos.stream()
                .filter(p -> "PAGADO".equals(p.getEstado()) || "EN_PROCESO".equals(p.getEstado()))
                .count());
        d.setProductosStockBajo(contarStockBajo());
        d.setTicketsAbiertos(soporteClient.contarTicketsAbiertos());

        d.setTicketPromedio(Calculos.ticketPromedio(totalMes, ventasMes.size()));
        List<ProductoVendidoDTO> vendidos = Calculos.productosVendidos(ventasMes, null);
        d.setProductoMasVendido(vendidos.isEmpty() ? null : vendidos.get(0).getNombre());
        d.setNuevosClientesHoy(usuariosClient.contarRegistradosHoy());
        d.setDevolucionesAbiertas(ventasClient.contarDevolucionesPendientes());
        return d;
    }

    /** HU-51: monto vendido en cada uno de los últimos 7 días, incluido hoy. Los días sin ventas van en 0. */
    public List<PuntoGraficoDTO> ventasUltimos7Dias(String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        LocalDate hoy = LocalDate.now();
        LocalDate desde = hoy.minusDays(6);
        List<PedidoDTO> ventas = Calculos.ventasEntre(ventasClient.obtenerPedidos(desde, hoy), desde, hoy);

        Map<LocalDate, Long> porDia = new LinkedHashMap<>();
        for (LocalDate dia = desde; !dia.isAfter(hoy); dia = dia.plusDays(1)) {
            porDia.put(dia, 0L);
        }
        for (PedidoDTO v : ventas) {
            porDia.merge(v.getFecha().toLocalDate(), (long) v.getTotal(), Long::sum);
        }
        List<PuntoGraficoDTO> puntos = new ArrayList<>();
        porDia.forEach((dia, monto) -> puntos.add(new PuntoGraficoDTO(dia.toString(), monto)));
        return puntos;
    }

    /** HU-53: monto vendido por categoría en el mes en curso. Las categorías sin ventas no aparecen. */
    public List<PuntoGraficoDTO> ventasPorCategoria(String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        Map<String, Long> porCategoria = new LinkedHashMap<>();
        for (PedidoDTO v : ventasDelMes()) {
            for (DetalleDTO d : v.getDetalles()) {
                porCategoria.merge(d.getCategoria() == null ? "SIN_CATEGORIA" : d.getCategoria(),
                        (long) d.subtotal(), Long::sum);
            }
        }
        return ordenarDeMayorAMenor(porCategoria);
    }

    /** HU-54: los 5 productos con más unidades vendidas en el mes. Si hay menos, solo esos. */
    public List<ProductoVendidoDTO> topProductos(String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        return Calculos.productosVendidos(ventasDelMes(), null).stream().limit(5).toList();
    }

    /** HU-55: cantidad de ventas del mes por región de envío, de mayor a menor. */
    public List<PuntoGraficoDTO> pedidosPorRegion(String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        Map<String, Long> porRegion = new LinkedHashMap<>();
        for (PedidoDTO v : ventasDelMes()) {
            porRegion.merge(v.getRegion() == null ? "Sin región" : v.getRegion(), 1L, Long::sum);
        }
        return ordenarDeMayorAMenor(porRegion);
    }

    /** HU-56: productos bajo su stock mínimo, del más urgente al menos urgente. */
    public List<StockBajoDTO> productosStockBajo(String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        return productosClient.obtenerStockBajo().stream()
                .map(p -> new StockBajoDTO(p.getSku(), p.getModelo(), p.getCategoria(), p.getStockDisponible(),
                        p.getStockMinimo(), nivel(p)))
                .toList();
    }

    // ---------- Apoyo ----------

    private List<PedidoDTO> ventasDelMes() {
        LocalDate hoy = LocalDate.now();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        return Calculos.ventasEntre(ventasClient.obtenerPedidos(inicioMes, hoy), inicioMes, hoy);
    }

    /** En la tarjeta KPI el stock bajo es secundario: si ms-productos no responde, queda null. */
    private Long contarStockBajo() {
        try {
            return (long) productosClient.obtenerStockBajo().size();
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** CRITICO si está agotado o tiene la mitad del mínimo o menos; si no, BAJO. */
    private String nivel(ProductoDTO p) {
        int stock = p.getStockDisponible() == null ? 0 : p.getStockDisponible();
        int minimo = p.getStockMinimo() == null ? 0 : p.getStockMinimo();
        return stock == 0 || stock * 2 <= minimo ? "CRITICO" : "BAJO";
    }

    private List<PuntoGraficoDTO> ordenarDeMayorAMenor(Map<String, Long> valores) {
        return valores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .map(e -> new PuntoGraficoDTO(e.getKey(), e.getValue()))
                .toList();
    }
}
