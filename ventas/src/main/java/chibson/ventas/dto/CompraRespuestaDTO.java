package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Resultado de pagar: si se aprobó, trae el comprobante; si no, el motivo del rechazo (HU-23, HU-24). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompraRespuestaDTO {
    private boolean aprobado;
    private String mensaje;
    private PedidoDTO pedido;
}
