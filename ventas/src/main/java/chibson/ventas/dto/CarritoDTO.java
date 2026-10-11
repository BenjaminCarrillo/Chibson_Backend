package chibson.ventas.dto;

import chibson.ventas.model.Carrito;
import chibson.ventas.model.ItemCarrito;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Carro con subtotales y total ya calculados. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarritoDTO {
    private List<Linea> items;
    /** Unidades en el carro, para el contador del ícono (HU-20). */
    private int cantidadProductos;
    private int total;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Linea {
        private Long idItem;
        private String sku;
        private String nombreProducto;
        private Integer cantidad;
        private Integer precioUnitario;
        private int subtotal;
    }

    public static CarritoDTO desde(Carrito carrito) {
        List<Linea> lineas = carrito.getItems().stream()
                .map(i -> new Linea(i.getIdItem(), i.getSku(), i.getNombreProducto(), i.getCantidad(),
                        i.getPrecioUnitario(), i.subtotal()))
                .toList();
        int unidades = carrito.getItems().stream().mapToInt(ItemCarrito::getCantidad).sum();
        return new CarritoDTO(lineas, unidades, carrito.calcularTotal());
    }
}
