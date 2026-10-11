package chibson.informes.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Indicadores del dashboard (HU-50 y HU-52). No se guarda: se calcula cada vez que se pide,
 * con los datos de los otros microservicios.
 * Un valor null significa que el microservicio que lo entrega no respondió.
 */
@Data
@NoArgsConstructor
public class Dashboard {

    // ---------- Tarjetas KPI (HU-50) ----------
    private Integer ventasDia;
    private Integer cantidadVentasDia;
    private Integer ventasMes;
    /** % respecto al mismo rango de días del mes anterior. null si el mes anterior no tuvo ventas. */
    private Double variacionMes;
    /** Pedidos pagados que todavía no se despachan (PAGADO o EN_PROCESO). */
    private Long pedidosPendientes;
    private Long productosStockBajo;
    private Long ticketsAbiertos;

    // ---------- Resumen rápido (HU-52) ----------
    /** Ticket promedio del mes en curso. */
    private Integer ticketPromedio;
    /** Producto con más unidades vendidas en el mes. null si no hubo ventas. */
    private String productoMasVendido;
    private Long nuevosClientesHoy;
    private Long devolucionesAbiertas;
}
