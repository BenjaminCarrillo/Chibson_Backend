package chibson.ventas.dto;

import chibson.ventas.model.DetallePedido;
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
    private MetodoPago metodoPago;
    private EstadoPedido estadoPedido;
    private List<DetallePedido> detalles;
}
