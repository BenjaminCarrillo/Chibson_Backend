package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Stock de un SKU; lo consultan ms-ventas y ms-informes. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InventarioDTO {
    private String sku;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private boolean bajoStock;
}
