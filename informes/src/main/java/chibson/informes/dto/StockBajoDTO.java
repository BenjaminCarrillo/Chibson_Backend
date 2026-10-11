package chibson.informes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Fila de la tabla de stock bajo (HU-56). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockBajoDTO {
    private String sku;
    private String modelo;
    private String categoria;
    private Integer stockDisponible;
    private Integer stockMinimo;
    /** CRITICO (agotado o en la mitad del mínimo o menos) o BAJO. El frontend lo pinta rojo o amarillo. */
    private String nivel;
}
