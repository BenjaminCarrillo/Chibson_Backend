package chibson.informes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Un punto o una barra de un gráfico: día, categoría o región, y su valor. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PuntoGraficoDTO {
    private String etiqueta;
    private long valor;
}
