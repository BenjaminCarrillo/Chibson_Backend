package chibson.informes.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resumen de un pedido que entrega ms-ventas (GET /api/pedidos/resumen). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long numeroPedido;
    private LocalDateTime fecha;
    private String estado;
    private Integer total;
    private String region;
    private boolean pagado;
    private List<DetalleDTO> detalles = new ArrayList<>();

    /** Una venta es un pedido con pago aprobado que no fue cancelado. */
    public boolean esVenta() {
        return pagado && !"CANCELADO".equals(estado);
    }
}
