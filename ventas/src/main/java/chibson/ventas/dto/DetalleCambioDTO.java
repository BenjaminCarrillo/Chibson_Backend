package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un campo modificado, con su valor anterior y nuevo (HU-73). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCambioDTO {
    private String campo;
    private String valorAnterior;
    private String valorNuevo;
}
