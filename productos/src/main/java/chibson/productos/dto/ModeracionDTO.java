package chibson.productos.dto;

import chibson.productos.model.EstadoResena;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Decisión del administrador sobre una reseña pendiente: APROBADA o RECHAZADA (HU-86). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModeracionDTO {
    private EstadoResena estado;
}
