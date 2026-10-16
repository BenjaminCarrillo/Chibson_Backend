package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Stock que devuelve ms-productos al descontar o reponer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    private String sku;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private Boolean bajoStock;
}
