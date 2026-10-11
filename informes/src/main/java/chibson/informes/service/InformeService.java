package chibson.informes.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import chibson.informes.client.UsuariosClient;
import chibson.informes.client.VentasClient;
import chibson.informes.dto.PedidoDTO;
import chibson.informes.dto.ProductoVendidoDTO;
import chibson.informes.model.Informe;
import chibson.informes.model.Periodo;
import chibson.informes.model.TipoInforme;

/** Informes de ventas, estado de pedidos y productos más vendidos (HU-45 a HU-47). */
@Service
public class InformeService {

    /** Estados de pedido de ms-ventas, en el orden del flujo. */
    private static final List<String> ESTADOS =
            List.of("PENDIENTE", "PAGADO", "EN_PROCESO", "ENVIADO", "ENTREGADO", "CANCELADO");

    /** Rangos más largos que esto se agrupan por mes en vez de por día. */
    private static final long DIAS_MAXIMOS_POR_DIA = 62;

    private final VentasClient ventasClient;
    private final UsuariosClient usuariosClient;

    public InformeService(VentasClient ventasClient, UsuariosClient usuariosClient) {
        this.ventasClient = ventasClient;
        this.usuariosClient = usuariosClient;
    }

    /**
     * HU-45: total de ventas, número de pedidos y ticket promedio del periodo.
     * DIARIO, MENSUAL y ANUAL toman el día, mes o año de "desde" (hoy si no se indica).
     * PERSONALIZADO necesita desde y hasta.
     */
    public Informe ventasPorPeriodo(String rutAdmin, Periodo periodo, LocalDate desde, LocalDate hasta) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        if (periodo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe elegir un periodo: DIARIO, MENSUAL, ANUAL o PERSONALIZADO");
        }
        LocalDate referencia = desde == null ? LocalDate.now() : desde;
        switch (periodo) {
            case DIARIO -> hasta = desde = referencia;
            case MENSUAL -> {
                desde = referencia.withDayOfMonth(1);
                hasta = YearMonth.from(referencia).atEndOfMonth();
            }
            case ANUAL -> {
                desde = referencia.withDayOfYear(1);
                hasta = referencia.withDayOfYear(referencia.lengthOfYear());
            }
            case PERSONALIZADO -> {
                if (desde == null || hasta == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Para un periodo personalizado debe indicar desde y hasta");
                }
            }
        }
        validarRango(desde, hasta);

        List<PedidoDTO> ventas = Calculos.ventasEntre(ventasClient.obtenerPedidos(desde, hasta), desde, hasta);
        int total = Calculos.sumarTotales(ventas);

        Informe informe = nuevo(TipoInforme.VENTAS_POR_PERIODO, desde, hasta);
        informe.setTotalVentas(total);
        informe.setNumeroPedidos((long) ventas.size());
        informe.setTicketPromedio(Calculos.ticketPromedio(total, ventas.size()));

        // Una fila por día con ventas, o por mes si el rango es largo
        boolean porMes = ChronoUnit.DAYS.between(desde, hasta) > DIAS_MAXIMOS_POR_DIA;
        Map<String, int[]> grupos = new TreeMap<>();
        for (PedidoDTO v : ventas) {
            LocalDate fecha = v.getFecha().toLocalDate();
            String clave = porMes ? YearMonth.from(fecha).toString() : fecha.toString();
            int[] acumulado = grupos.computeIfAbsent(clave, k -> new int[2]);
            acumulado[0]++;
            acumulado[1] += v.getTotal();
        }
        grupos.forEach((clave, acumulado) -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put(porMes ? "mes" : "fecha", clave);
            fila.put("pedidos", acumulado[0]);
            fila.put("total", acumulado[1]);
            fila.put("ticketPromedio", Calculos.ticketPromedio(acumulado[1], acumulado[0]));
            informe.getFilas().add(fila);
        });
        if (ventas.isEmpty()) {
            informe.setMensaje("No hay ventas registradas entre " + desde + " y " + hasta);
        }
        return informe;
    }

    /**
     * HU-46: pedidos agrupados por estado, con el número de cada pedido para abrir su detalle.
     * Las fechas son opcionales; sin ellas se incluyen todos los pedidos.
     */
    public Informe estadoPedidos(String rutAdmin, LocalDate desde, LocalDate hasta) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        validarRango(desde, hasta);
        List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(desde, hasta).stream()
                .filter(p -> Calculos.entre(p, desde, hasta))
                .toList();

        Informe informe = nuevo(TipoInforme.ESTADO_PEDIDOS, desde, hasta);
        informe.setNumeroPedidos((long) pedidos.size());
        for (String estado : ESTADOS) {
            List<PedidoDTO> delEstado = pedidos.stream().filter(p -> estado.equals(p.getEstado())).toList();
            List<Map<String, Object>> lista = new ArrayList<>();
            for (PedidoDTO p : delEstado) {
                Map<String, Object> resumen = new LinkedHashMap<>();
                resumen.put("numeroPedido", p.getNumeroPedido());
                resumen.put("fecha", p.getFecha());
                resumen.put("total", p.getTotal());
                resumen.put("region", p.getRegion());
                lista.add(resumen);
            }
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("estado", estado);
            fila.put("cantidad", delEstado.size());
            fila.put("monto", Calculos.sumarTotales(delEstado));
            fila.put("pedidos", lista);
            informe.getFilas().add(fila);
        }
        if (pedidos.isEmpty()) {
            informe.setMensaje("No hay pedidos en el periodo seleccionado");
        }
        return informe;
    }

    /**
     * HU-47: ranking de productos por unidades vendidas, con sus ingresos.
     * Sin fechas se usa el mes en curso. La categoría es opcional.
     */
    public Informe productosMasVendidos(String rutAdmin, LocalDate desde, LocalDate hasta, String categoria) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = desde == null ? hoy.withDayOfMonth(1) : desde;
        LocalDate fin = hasta == null ? hoy : hasta;
        validarRango(inicio, fin);
        String filtro = categoria == null || categoria.isBlank() ? null : categoria.trim().toUpperCase();

        List<PedidoDTO> ventas = Calculos.ventasEntre(ventasClient.obtenerPedidos(inicio, fin), inicio, fin);
        List<ProductoVendidoDTO> ranking = Calculos.productosVendidos(ventas, filtro);

        Informe informe = nuevo(TipoInforme.PRODUCTOS_MAS_VENDIDOS, inicio, fin);
        informe.setCategoria(filtro);
        informe.setTotalVentas(ranking.stream().mapToInt(ProductoVendidoDTO::getIngresos).sum());
        informe.setNumeroPedidos(ventas.stream()
                .filter(v -> v.getDetalles().stream()
                        .anyMatch(d -> filtro == null || filtro.equalsIgnoreCase(d.getCategoria())))
                .count());
        int posicion = 1;
        for (ProductoVendidoDTO p : ranking) {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("posicion", posicion++);
            fila.put("sku", p.getSku());
            fila.put("producto", p.getNombre());
            fila.put("categoria", p.getCategoria());
            fila.put("unidades", p.getUnidades());
            fila.put("ingresos", p.getIngresos());
            informe.getFilas().add(fila);
        }
        if (ranking.isEmpty()) {
            informe.setMensaje("No hay productos vendidos en el periodo seleccionado"
                    + (filtro == null ? "" : " para la categoría " + filtro));
        }
        return informe;
    }

    // ---------- Apoyo ----------

    private Informe nuevo(TipoInforme tipo, LocalDate desde, LocalDate hasta) {
        Informe informe = new Informe();
        informe.setTipo(tipo);
        informe.setFechaDesde(desde);
        informe.setFechaHasta(hasta);
        return informe;
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a la fecha hasta");
        }
    }
}
