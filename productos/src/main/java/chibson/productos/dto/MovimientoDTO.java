package chibson.productos.dto;

import chibson.productos.model.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que envía ms-ventas para descontar o reponer stock (HU-63). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoDTO {
    private Integer cantidad;
    private Long numeroPedido;
    /** Solo al reponer: DEVOLUCION o CANCELACION. */
    private TipoMovimiento tipo;
}
