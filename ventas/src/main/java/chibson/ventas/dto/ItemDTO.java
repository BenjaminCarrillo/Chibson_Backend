package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto y cantidad que el frontend envía al comprar (HU-20 a HU-23). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {
    private String sku;
    private Integer cantidad;
}
