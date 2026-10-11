package chibson.soporte.dto;

import chibson.soporte.model.EstadoTicket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Respuesta del administrador (HU-29). El estado es opcional: si viene, se cambia junto con la respuesta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaDTO {
    private String contenido;
    private EstadoTicket estado;
}
