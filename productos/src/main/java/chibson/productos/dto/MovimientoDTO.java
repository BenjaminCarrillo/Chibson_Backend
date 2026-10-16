package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que envía ms-ventas para descontar o reponer stock. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoDTO {
    private Integer cantidad;
    private Long numeroPedido;
    /** Solo al reponer: DEVOLUCION o CANCELACION. */
    private String tipo;
    /** Quién provocó el cambio: el cliente que compró o el administrador que canceló o aprobó. */
    private String rutResponsable;
}
