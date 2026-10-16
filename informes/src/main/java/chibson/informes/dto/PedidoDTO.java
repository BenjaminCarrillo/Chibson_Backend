package chibson.informes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Resumen de un pedido que entrega ms-ventas (GET /api/pedidos/resumen). El total es bruto;
 * los reembolsos vienen aparte, con su fecha.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long numeroPedido;
    private LocalDateTime fecha;
    private String estado;
    private Integer total;
    private String region;
    private Boolean pagado;
    private List<ItemDTO> items = new ArrayList<>();
    private List<ReembolsoDTO> reembolsos = new ArrayList<>();

    /** Una venta es un pedido con pago aprobado que no fue cancelado. */
    public boolean esVenta() {
        return Boolean.TRUE.equals(pagado) && !"CANCELADO".equals(estado);
    }
}
