package chibson.productos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta pública del administrador a una reseña (HU-87). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaResenaDTO {
    private String respuesta;
}
