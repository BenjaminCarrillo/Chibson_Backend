package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Reseña que escribe el cliente (HU-81). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResenaDTO {
    private String sku;
    private Integer calificacion;
    private String titulo;
    /** Opcional. */
    private String comentario;
}
