package chibson.informes.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Línea de un pedido que entrega ms-ventas. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleDTO {
    private String sku;
    private String nombreProducto;
    private String categoria;
    private Integer cantidad;
    private Integer precioUnitario;

    public int subtotal() {
        return cantidad * precioUnitario;
    }
}
