package chibson.soporte.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import chibson.soporte.model.CategoriaTicket;
import chibson.soporte.model.EstadoTicket;
import chibson.soporte.model.MensajeTicket;
import chibson.soporte.model.Ticket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Ticket con el nombre del cliente y su conversación (HU-27, HU-28). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketRespuestaDTO {
    private Long numeroTicket;
    private String rutCliente;
    private String nombreCliente;
    private String asunto;
    private CategoriaTicket categoria;
    private String descripcion;
    private String adjuntoUrl;
    private EstadoTicket estado;
    private Integer calificacion;
    private String comentarioCalificacion;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaResolucion;
    private List<MensajeTicket> mensajes;

    /** cliente puede ser null si ms-usuarios no respondió. */
    public static TicketRespuestaDTO desde(Ticket t, UsuarioDTO cliente) {
        return new TicketRespuestaDTO(t.getNumeroTicket(), t.getRutCliente(),
                cliente == null ? null : cliente.nombreCompleto(), t.getAsunto(), t.getCategoria(),
                t.getDescripcion(), t.getAdjuntoUrl(), t.getEstado(), t.getCalificacion(),
                t.getComentarioCalificacion(), t.getFechaCreacion(), t.getFechaResolucion(),
                new ArrayList<>(t.getMensajes()));
    }
}
