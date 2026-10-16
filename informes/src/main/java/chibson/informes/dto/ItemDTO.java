package chibson.informes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Producto de un pedido, tal como lo entrega ms-ventas. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ItemDTO {
    private String sku;
    private String nombreProducto;
    private String categoria;
    private Integer cantidad;
    private Integer precioUnitario;

    public int subtotal() {
        return cantidad * precioUnitario;
    }
}
