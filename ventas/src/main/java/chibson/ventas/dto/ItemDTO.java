package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto y cantidad que el cliente agrega o modifica en el carro (HU-20, HU-21). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {
    private String sku;
    private Integer cantidad;
}
