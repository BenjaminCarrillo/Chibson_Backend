package chibson.informes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto con su stock, tal como lo entrega ms-productos en /api/productos/bajo-stock. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private String sku;
    private String modelo;
    private String categoria;
    private Integer stockDisponible;
    private Integer stockMinimo;
}
