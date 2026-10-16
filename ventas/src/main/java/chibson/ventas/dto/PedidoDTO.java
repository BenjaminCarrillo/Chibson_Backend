package chibson.ventas.dto;

import chibson.ventas.model.ItemPedido;
import chibson.ventas.model.EstadoPedido;
import chibson.ventas.model.HistorialEstadoPedido;
import chibson.ventas.model.MetodoPago;
import chibson.ventas.model.Pago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Pedido completo para mostrar: comprobante (HU-24), detalle (HU-33) e historial (HU-36). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PedidoDTO {
    private Long numeroPedido;
    private String rutCliente;
    private String nombreCliente;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private Integer subtotal;
    private Integer costoEnvio;
    private Integer iva;
    private Integer total;
    private MetodoPago metodoPago;
    private DireccionDTO direccion;
    private String motivoCancelacion;
    private List<ItemPedido> items;
    private List<HistorialEstadoPedido> historial;
    private List<Pago> pagos;
}
