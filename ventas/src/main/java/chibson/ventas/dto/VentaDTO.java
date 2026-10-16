package chibson.ventas.dto;

import chibson.ventas.model.ItemPedido;
import chibson.ventas.model.EstadoPedido;
import chibson.ventas.model.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Fila de Ventas.html y su detalle (HU-57 a HU-59). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaDTO {
    private Long numeroVenta;
    private String rutCliente;
    private String nombreCliente;
    private LocalDateTime fecha;
    private Integer total;
    /** 0 si no tiene devoluciones reembolsadas (HU-57 criterio 4). */
    private Integer montoReembolsado;
    /** total - montoReembolsado */
    private Integer totalNeto;
    private MetodoPago metodoPago;
    private EstadoPedido estadoPedido;
    private List<ItemPedido> items;
    /** Productos y cantidades reembolsados (HU-59 criterio 4); vacío si no hay. */
    private List<ReembolsoDTO> reembolsos;
}
