package chibson.informes.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import chibson.informes.client.ProductosClient;
import chibson.informes.client.SoporteClient;
import chibson.informes.client.UsuariosClient;
import chibson.informes.client.VentasClient;
import chibson.informes.dto.ItemDTO;
import chibson.informes.dto.PedidoDTO;
import chibson.informes.dto.ProductoDTO;
import chibson.informes.dto.ProductoVendidoDTO;
import chibson.informes.dto.ReembolsoDTO;
import chibson.informes.model.FilaReporte;
import chibson.informes.model.Reporte;

/**
 * Arma el contenido de cada tipo de reporte con los datos de los demás microservicios.
 * No guarda nada ni revisa permisos: eso lo hace ReporteService.
 */
@Component
public class GeneradorReportes {

    /** Estados de pedido de ms-ventas, en el orden del flujo. */
    private static final List<String> ESTADOS =
            List.of("PENDIENTE", "PAGADO", "EN_PROCESO", "ENVIADO", "ENTREGADO", "CANCELADO");

    /** Rangos más largos que esto se agrupan por mes en vez de por día. */
    private static final long DIAS_MAXIMOS_POR_DIA = 62;

    private final VentasClient ventasClient;
    private final ProductosClient productosClient;
    private final UsuariosClient usuariosClient;
    private final SoporteClient soporteClient;

    public GeneradorReportes(VentasClient ventasClient, ProductosClient productosClient,
                             UsuariosClient usuariosClient, SoporteClient soporteClient) {
        this.ventasClient = ventasClient;
        this.productosClient = productosClient;
        this.usuariosClient = usuariosClient;
        this.soporteClient = soporteClient;
    }

    /** HU-45: ventas netas, número de ventas y ticket promedio, con una fila por día (o por mes). */
    public void ventasPorPeriodo(Reporte reporte) {
        LocalDate desde = reporte.getFechaDesde();
        LocalDate hasta = reporte.getFechaHasta();
        List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(desde, hasta);
        List<PedidoDTO> ventas = Calculos.ventasEntre(pedidos, desde, hasta);
        List<ReembolsoDTO> reembolsos = Calculos.reembolsosEntre(pedidos, desde, hasta);
        int neto = Calculos.totalNeto(ventas, reembolsos);

        reporte.setTotalVentas(neto);
        reporte.setNumeroPedidos((long) ventas.size());
        reporte.setTicketPromedio(Calculos.ticketPromedio(neto, ventas.size()));

        reporte.agregarFila("TOTALES", "Ventas brutas", (long) ventas.size(), Calculos.sumarTotales(ventas));
        reporte.agregarFila("TOTALES", "Reembolsos", Calculos.unidadesReembolsadas(reembolsos),
                -Calculos.sumarReembolsos(reembolsos));
        reporte.agregarFila("TOTALES", "Ventas netas", (long) ventas.size(), neto);

        boolean porMes = ChronoUnit.DAYS.between(desde, hasta) > DIAS_MAXIMOS_POR_DIA;
        String seccion = porMes ? "POR_MES" : "POR_DIA";
        Map<String, long[]> grupos = new TreeMap<>();
        for (PedidoDTO v : ventas) {
            long[] acumulado = grupos.computeIfAbsent(clave(v.getFecha().toLocalDate(), porMes), k -> new long[2]);
            acumulado[0]++;
            acumulado[1] += v.getTotal();
        }
        for (ReembolsoDTO r : reembolsos) {
            long[] acumulado = grupos.computeIfAbsent(clave(r.getFechaReembolso().toLocalDate(), porMes), k -> new long[2]);
            acumulado[1] -= r.getMonto();
        }
        grupos.forEach((fecha, acumulado) -> reporte.agregarFila(seccion, fecha, acumulado[0], (int) acumulado[1]));

        if (ventas.isEmpty() && reembolsos.isEmpty()) {
            reporte.setMensaje("No hay ventas registradas entre " + desde + " y " + hasta);
        }
    }

    /** HU-46: pedidos agrupados por estado; cada pedido trae su número para abrir el detalle. */
    public void estadoPedidos(Reporte reporte) {
        LocalDate desde = reporte.getFechaDesde();
        LocalDate hasta = reporte.getFechaHasta();
        List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(desde, hasta).stream()
                .filter(p -> Calculos.entre(p.getFecha(), desde, hasta))
                .toList();
        reporte.setNumeroPedidos((long) pedidos.size());

        for (String estado : ESTADOS) {
            List<PedidoDTO> delEstado = pedidos.stream().filter(p -> estado.equals(p.getEstado())).toList();
            reporte.agregarFila("RESUMEN", estado, (long) delEstado.size(), Calculos.sumarTotales(delEstado));
        }
        for (String estado : ESTADOS) {
            for (PedidoDTO p : pedidos) {
                if (estado.equals(p.getEstado())) {
                    FilaReporte fila = reporte.agregarFila(estado, "Pedido #" + p.getNumeroPedido(), null, p.getTotal());
                    fila.setReferencia(String.valueOf(p.getNumeroPedido()));
                    fila.setValor(p.getFecha().toLocalDate() + (p.getRegion() == null ? "" : " · " + p.getRegion()));
                }
            }
        }
        if (pedidos.isEmpty()) {
            reporte.setMensaje("No hay pedidos entre " + desde + " y " + hasta);
        }
    }

    /** HU-47: ranking por unidades vendidas netas, con ingresos. Categoría opcional. */
    public void productosMasVendidos(Reporte reporte) {
        LocalDate desde = reporte.getFechaDesde();
        LocalDate hasta = reporte.getFechaHasta();
        String categoria = reporte.getCategoria();
        List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(desde, hasta);
        List<PedidoDTO> ventas = Calculos.ventasEntre(pedidos, desde, hasta);
        List<ReembolsoDTO> reembolsos = Calculos.reembolsosEntre(pedidos, desde, hasta);
        List<ProductoVendidoDTO> ranking = Calculos.productosVendidos(ventas, reembolsos,
                Calculos.productosPorSku(pedidos), categoria);

        reporte.setTotalVentas(ranking.stream().mapToInt(ProductoVendidoDTO::getIngresos).sum());
        reporte.setNumeroPedidos(ventas.stream()
                .filter(v -> v.getItems().stream()
                        .anyMatch(i -> categoria == null || categoria.equalsIgnoreCase(i.getCategoria())))
                .count());
        for (ProductoVendidoDTO p : ranking) {
            agregarProducto(reporte, "RANKING", p);
        }
        if (ranking.isEmpty()) {
            reporte.setMensaje("No hay productos vendidos entre " + desde + " y " + hasta
                    + (categoria == null ? "" : " en la categoría " + categoria));
        }
    }

    /**
     * HU-50: indicadores clave del periodo comparados con el periodo anterior, top 5, ventas por
     * categoría, pedidos por región y productos con stock bajo. Los indicadores de estado (pedidos
     * pendientes, stock bajo, tickets y devoluciones) son los del momento en que se genera.
     */
    public void resumenGeneral(Reporte reporte, LocalDate anteriorDesde, LocalDate anteriorHasta) {
        LocalDate desde = reporte.getFechaDesde();
        LocalDate hasta = reporte.getFechaHasta();
        List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(anteriorDesde, hasta);
        List<PedidoDTO> ventas = Calculos.ventasEntre(pedidos, desde, hasta);
        List<ReembolsoDTO> reembolsos = Calculos.reembolsosEntre(pedidos, desde, hasta);
        int neto = Calculos.totalNeto(ventas, reembolsos);
        int netoAnterior = Calculos.totalNeto(Calculos.ventasEntre(pedidos, anteriorDesde, anteriorHasta),
                Calculos.reembolsosEntre(pedidos, anteriorDesde, anteriorHasta));
        Map<String, ItemDTO> productos = Calculos.productosPorSku(pedidos);
        List<ProductoVendidoDTO> ranking = Calculos.productosVendidos(ventas, reembolsos, productos, null);

        reporte.setTotalVentas(neto);
        reporte.setNumeroPedidos((long) ventas.size());
        reporte.setTicketPromedio(Calculos.ticketPromedio(neto, ventas.size()));

        // Indicadores
        reporte.agregarFila("INDICADORES", "Ventas netas del periodo", (long) ventas.size(), neto);
        Double variacion = Calculos.variacion(neto, netoAnterior);
        reporte.agregarFila("INDICADORES", "Variación respecto al periodo anterior", null, netoAnterior)
                .setValor(variacion == null ? "Sin ventas en el periodo anterior"
                        : (variacion > 0 ? "+" : "") + variacion + " %");
        reporte.agregarFila("INDICADORES", "Reembolsos del periodo", Calculos.unidadesReembolsadas(reembolsos),
                Calculos.sumarReembolsos(reembolsos));
        reporte.agregarFila("INDICADORES", "Ticket promedio", null, reporte.getTicketPromedio());
        reporte.agregarFila("INDICADORES", "Producto más vendido",
                        ranking.isEmpty() ? 0L : (long) ranking.get(0).getUnidades(), null)
                .setValor(ranking.isEmpty() ? "Sin ventas en el periodo" : ranking.get(0).getNombre());
        indicador(reporte, "Clientes nuevos", usuariosClient.contarClientesRegistrados(desde, hasta), "ms-usuarios");
        indicador(reporte, "Pedidos pendientes de despacho (pagados o en proceso)", contarPorDespachar(), "ms-ventas");
        List<ProductoDTO> stockBajo = obtenerStockBajo();
        indicador(reporte, "Productos con stock bajo", stockBajo == null ? null : (long) stockBajo.size(), "ms-productos");
        indicador(reporte, "Tickets de soporte abiertos", soporteClient.contarTicketsAbiertos(), "ms-soporte");
        indicador(reporte, "Devoluciones pendientes", ventasClient.contarDevolucionesPendientes(), "ms-ventas");

        // Top 5, categorías y regiones
        ranking.stream().limit(5).forEach(p -> agregarProducto(reporte, "TOP_5", p));
        Calculos.ventasPorCategoria(ventas, reembolsos, productos)
                .forEach((categoria, monto) -> reporte.agregarFila("POR_CATEGORIA", categoria, null, monto));
        Map<String, long[]> porRegion = new LinkedHashMap<>();
        for (PedidoDTO v : ventas) {
            long[] acumulado = porRegion.computeIfAbsent(v.getRegion() == null ? "Sin región" : v.getRegion(),
                    k -> new long[2]);
            acumulado[0]++;
            acumulado[1] += v.getTotal();
        }
        porRegion.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .forEach(e -> reporte.agregarFila("POR_REGION", e.getKey(), e.getValue()[0], (int) e.getValue()[1]));

        if (stockBajo != null) {
            for (ProductoDTO p : stockBajo) {
                long stock = p.getStockDisponible() == null ? 0L : p.getStockDisponible();
                FilaReporte fila = reporte.agregarFila("STOCK_BAJO", p.getModelo(), stock, null);
                fila.setReferencia(p.getSku());
                fila.setValor("mínimo " + p.getStockMinimo());
            }
        }
        if (ventas.isEmpty() && reembolsos.isEmpty()) {
            reporte.setMensaje("No hubo ventas entre " + desde + " y " + hasta);
        }
    }

    // ---------- Apoyo ----------

    private String clave(LocalDate fecha, boolean porMes) {
        return porMes ? YearMonth.from(fecha).toString() : fecha.toString();
    }

    private void agregarProducto(Reporte reporte, String seccion, ProductoVendidoDTO p) {
        FilaReporte fila = reporte.agregarFila(seccion, p.getNombre(), (long) p.getUnidades(), p.getIngresos());
        fila.setReferencia(p.getSku());
        fila.setValor(p.getCategoria());
    }

    /** Un indicador que viene de otro microservicio: si no respondió, se muestra un texto, nunca vacío (HU-50). */
    private void indicador(Reporte reporte, String etiqueta, Long valor, String servicio) {
        reporte.agregarFila("INDICADORES", etiqueta, valor, null)
                .setValor(valor == null ? "Sin datos: " + servicio + " no respondió" : null);
    }

    private Long contarPorDespachar() {
        try {
            return ventasClient.obtenerPedidos(null, null).stream()
                    .filter(p -> "PAGADO".equals(p.getEstado()) || "EN_PROCESO".equals(p.getEstado()))
                    .count();
        } catch (ResponseStatusException e) {
            return null;
        }
    }

    private List<ProductoDTO> obtenerStockBajo() {
        try {
            return new ArrayList<>(productosClient.obtenerStockBajo());
        } catch (ResponseStatusException e) {
            return null;
        }
    }
}
