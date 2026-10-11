package chibson.ventas.dto;

import java.time.LocalDateTime;
import java.util.List;

import chibson.ventas.model.DetallePedido;
import chibson.ventas.model.EstadoPedido;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos de un pedido que necesita ms-informes para el dashboard y los informes (HU-45 a HU-55). */
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
    private List<DetallePedido> detalles;
}
