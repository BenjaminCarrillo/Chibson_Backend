package chibson.soporte.dto;

import chibson.soporte.model.EstadoTicket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cambio de estado sin respuesta (HU-31). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstadoDTO {
    private EstadoTicket estado;
}
