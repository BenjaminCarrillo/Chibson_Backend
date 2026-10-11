package chibson.informes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto con su stock, tal como lo entrega ms-productos en /api/inventario/bajo-stock. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private String sku;
    private String modelo;
    private String categoria;
    private Integer stockDisponible;
    private Integer stockMinimo;
}
