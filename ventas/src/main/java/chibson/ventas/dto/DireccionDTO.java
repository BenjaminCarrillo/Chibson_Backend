package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Dirección de envío del pedido: la alternativa o la del perfil. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DireccionDTO {
    private String region;
    private String ciudadComuna;
    private String condominio;
    private String numero;
    /** true si es una dirección distinta ingresada solo para este pedido. */
    private boolean alternativa;
}
