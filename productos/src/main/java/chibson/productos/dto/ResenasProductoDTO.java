package chibson.productos.dto;

import chibson.productos.model.Resena;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Reseñas públicas de un producto con su promedio y total (HU-82). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenasProductoDTO {
    private String sku;
    /** Promedio redondeado a un decimal; 0 si no hay reseñas aprobadas. */
    private double promedio;
    private int total;
    private List<Resena> resenas;
}
