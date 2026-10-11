package chibson.ventas.dto;

import chibson.ventas.model.MetodoPago;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos de "Finalizar compra" (HU-23, HU-25). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutDTO {
    private MetodoPago metodoPago;
    /** true: usa los campos de abajo solo para este pedido. false: usa la dirección del perfil. */
    private Boolean usarDireccionDistinta;
    private String region;
    private String ciudadComuna;
    /** Opcional. */
    private String condominio;
    private String numero;
    /** Solo para pruebas: la pasarela simulada rechaza el pago. */
    private Boolean simularRechazo;
}
