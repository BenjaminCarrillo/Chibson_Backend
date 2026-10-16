package chibson.ventas.dto;

import java.time.LocalDateTime;
import java.util.List;

import chibson.ventas.model.ItemPedido;
import chibson.ventas.model.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de un pedido que necesita ms-informes para los reportes (HU-45 a HU-50).
 * Los montos son brutos; los reembolsos vienen aparte, con su fecha, para que el reporte los
 * descuente en el periodo en que se registraron (HU-45 criterio 4).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoResumenDTO {
    private Long numeroPedido;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private Integer total;
    /** Región de envío: la alternativa del pedido o la del perfil del cliente. */
    private String region;
    /** true si tiene un pago aprobado (es una venta). */
    private boolean pagado;
    private List<ItemPedido> items;
    private List<ReembolsoDTO> reembolsos;
}
