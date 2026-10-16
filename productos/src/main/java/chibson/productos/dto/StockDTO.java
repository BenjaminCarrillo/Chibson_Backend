package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Stock de un producto; lo consulta ms-ventas y es la respuesta al descontar o reponer. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockDTO {
    private String sku;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private Boolean bajoStock;
}
