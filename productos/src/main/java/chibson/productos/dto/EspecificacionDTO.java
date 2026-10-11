package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Una fila de la ficha técnica ya interpretada, por ejemplo "Cuerpo: Caoba maciza" (HU-19). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EspecificacionDTO {
    private String clave;
    private String valor;
}
