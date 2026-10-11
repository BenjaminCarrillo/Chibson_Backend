package chibson.ventas.dto;

import chibson.ventas.model.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reintento de pago de un pedido pendiente (HU-23, criterio 4). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private MetodoPago metodoPago;
    /** Solo para pruebas: la pasarela simulada rechaza el pago. */
    private boolean simularRechazo;
}
