package chibson.informes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto con sus unidades vendidas e ingresos (HU-47, HU-54). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoVendidoDTO {
    private String sku;
    private String nombre;
    private String categoria;
    private int unidades;
    private int ingresos;
}
