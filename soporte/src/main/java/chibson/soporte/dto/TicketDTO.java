package chibson.soporte.dto;

import chibson.soporte.model.CategoriaTicket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formulario de nueva solicitud de soporte (HU-26). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {
    private String asunto;
    private CategoriaTicket categoria;
    private String descripcion;
    /** Opcional. */
    private String adjuntoUrl;
}
