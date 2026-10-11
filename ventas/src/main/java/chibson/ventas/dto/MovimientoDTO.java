package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que se envía a ms-productos para descontar o reponer stock. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoDTO {
    private Integer cantidad;
    private Long numeroPedido;
    /** Solo al reponer: DEVOLUCION o CANCELACION. */
    private String tipo;
}
