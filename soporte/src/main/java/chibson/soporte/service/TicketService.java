package chibson.soporte.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.soporte.client.UsuariosClient;
import chibson.soporte.dto.CierreDTO;
import chibson.soporte.dto.DetalleCambioDTO;
import chibson.soporte.dto.RegistroAccionDTO;
import chibson.soporte.dto.RespuestaDTO;
import chibson.soporte.dto.TicketDTO;
import chibson.soporte.dto.TicketRespuestaDTO;
import chibson.soporte.dto.UsuarioDTO;
import chibson.soporte.model.EstadoTicket;
import chibson.soporte.model.MensajeTicket;
import chibson.soporte.model.Ticket;
import chibson.soporte.repository.TicketRepository;

/** Tickets de soporte (HU-26 a HU-32). */
@Service
public class TicketService {

    private static final String MODULO = "Soporte";

    private final TicketRepository ticketRepository;
    private final UsuariosClient usuariosClient;

    public TicketService(TicketRepository ticketRepository, UsuariosClient usuariosClient) {
        this.ticketRepository = ticketRepository;
        this.usuariosClient = usuariosClient;
    }

    // ---------- Cliente ----------

    /** HU-26: crea el ticket ABIERTO. La respuesta trae el número de ticket. */
    @Transactional
    public TicketRespuestaDTO crearTicket(String rutHeader, TicketDTO datos) {
        UsuarioDTO cliente = usuariosClient.obtenerUsuario(rutHeader);
        String asunto = obligatorio(datos.getAsunto(), "El asunto es obligatorio");
        if (asunto.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El asunto no puede superar los 150 caracteres");
        }
        if (datos.getCategoria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe seleccionar una categoría: DESPACHO, GARANTIA, PAGO, PEDIDO u OTRO");
        }
        Ticket ticket = new Ticket();
        ticket.setRutCliente(cliente.getRut());
        ticket.setAsunto(asunto);
        ticket.setCategoria(datos.getCategoria());
        ticket.setDescripcion(obligatorio(datos.getDescripcion(), "La descripción del problema es obligatoria"));
        ticket.setAdjuntoUrl(vacioANull(datos.getAdjuntoUrl()));
        ticket.setEstado(EstadoTicket.ABIERTO);
        ticket.setFechaCreacion(LocalDateTime.now());
        return TicketRespuestaDTO.desde(ticketRepository.save(ticket), cliente);
    }

    /** HU-27: tickets del cliente, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<TicketRespuestaDTO> listarPorCliente(String rutHeader) {
        UsuarioDTO cliente = usuariosClient.obtenerUsuario(rutHeader);
        return ticketRepository.findByRutClienteOrderByFechaCreacionDesc(cliente.getRut()).stream()
                .map(t -> TicketRespuestaDTO.desde(t, cliente))
                .toList();
    }

    /** HU-27: un ticket propio con su historial de respuestas. */
    @Transactional(readOnly = true)
    public TicketRespuestaDTO obtenerDetalleCliente(Long numeroTicket, String rutHeader) {
        UsuarioDTO cliente = usuariosClient.obtenerUsuario(rutHeader);
        return TicketRespuestaDTO.desde(buscarDelCliente(numeroTicket, cliente.getRut()), cliente);
    }

    /** HU-30: el cliente confirma el cierre de un ticket RESUELTO y puede calificar de 1 a 5. */
    @Transactional
    public TicketRespuestaDTO cerrarYCalificar(Long numeroTicket, String rutHeader, CierreDTO datos) {
        UsuarioDTO cliente = usuariosClient.obtenerUsuario(rutHeader);
        Ticket ticket = buscarDelCliente(numeroTicket, cliente.getRut());
        if (ticket.getEstado() != EstadoTicket.RESUELTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo puede cerrar un ticket resuelto. Estado actual: " + ticket.getEstado());
        }
        Integer calificacion = datos == null ? null : datos.getCalificacion();
        if (calificacion != null && (calificacion < 1 || calificacion > 5)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La calificación debe estar entre 1 y 5");
        }
        String comentario = datos == null ? null : vacioANull(datos.getComentario());
        if (comentario != null && comentario.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El comentario no puede superar los 500 caracteres");
        }
        ticket.setEstado(EstadoTicket.CERRADO);
        ticket.setCalificacion(calificacion);
        ticket.setComentarioCalificacion(comentario);
        return TicketRespuestaDTO.desde(ticketRepository.save(ticket), cliente);
    }

    // ---------- Administración ----------

    /** HU-28: todos los tickets, del más antiguo al más reciente, con filtro opcional por estado. */
    @Transactional(readOnly = true)
    public List<TicketRespuestaDTO> listarTickets(String rutAdmin, EstadoTicket estado) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_SOPORTE);
        List<Ticket> tickets = estado == null
                ? ticketRepository.findAllByOrderByFechaCreacionAsc()
                : ticketRepository.findByEstadoOrderByFechaCreacionAsc(estado);
        Map<String, UsuarioDTO> cache = new HashMap<>();
        return tickets.stream()
                .map(t -> TicketRespuestaDTO.desde(t,
                        cache.computeIfAbsent(t.getRutCliente(), usuariosClient::buscarUsuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public TicketRespuestaDTO obtenerDetalle(Long numeroTicket, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_SOPORTE);
        Ticket ticket = findById(numeroTicket);
        return TicketRespuestaDTO.desde(ticket, usuariosClient.buscarUsuario(ticket.getRutCliente()));
    }

    /** HU-29: agrega una respuesta a la conversación y, si se indica, cambia el estado. */
    @Transactional
    public TicketRespuestaDTO responderTicket(Long numeroTicket, RespuestaDTO datos, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_SOPORTE);
        String contenido = obligatorio(datos.getContenido(), "La respuesta no puede estar vacía");
        Ticket ticket = findById(numeroTicket);
        validarNoCerrado(ticket);

        MensajeTicket mensaje = new MensajeTicket();
        mensaje.setRutAutor(rutAdmin.trim());
        mensaje.setEsAdministrador(true);
        mensaje.setContenido(contenido);
        mensaje.setFecha(LocalDateTime.now());
        mensaje.setTicket(ticket);
        ticket.getMensajes().add(mensaje);

        EstadoTicket anterior = ticket.getEstado();
        EstadoTicket nuevo = datos.getEstado();
        boolean cambiaEstado = nuevo != null && nuevo != anterior;
        if (cambiaEstado) {
            cambiarEstado(ticket, nuevo);
        }
        ticket = ticketRepository.save(ticket);

        String aviso = "Su ticket #" + numeroTicket + " fue respondido";
        usuariosClient.enviarNotificacion(ticket.getRutCliente(),
                cambiaEstado ? aviso + ". Nuevo estado: " + nuevo : aviso, "ticket:" + numeroTicket);

        RegistroAccionDTO accion = accion(rutAdmin, "MODIFICAR", "Respondió el ticket #" + numeroTicket);
        if (cambiaEstado) {
            accion.getCambios().add(new DetalleCambioDTO("estado", anterior.name(), nuevo.name()));
        }
        usuariosClient.registrarAccion(accion);
        return TicketRespuestaDTO.desde(ticket, usuariosClient.buscarUsuario(ticket.getRutCliente()));
    }

    /** HU-31: cambia el estado sin escribir una respuesta y avisa al cliente. */
    @Transactional
    public TicketRespuestaDTO actualizarEstado(Long numeroTicket, EstadoTicket nuevo, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_SOPORTE);
        if (nuevo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nuevo estado es obligatorio");
        }
        Ticket ticket = findById(numeroTicket);
        validarNoCerrado(ticket);
        EstadoTicket anterior = ticket.getEstado();
        if (nuevo == anterior) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ticket ya está en estado " + nuevo);
        }
        cambiarEstado(ticket, nuevo);
        ticket = ticketRepository.save(ticket);

        usuariosClient.enviarNotificacion(ticket.getRutCliente(),
                "Su ticket #" + numeroTicket + " cambió de estado: " + nuevo, "ticket:" + numeroTicket);
        RegistroAccionDTO accion = accion(rutAdmin, "MODIFICAR", "Cambió el estado del ticket #" + numeroTicket);
        accion.getCambios().add(new DetalleCambioDTO("estado", anterior.name(), nuevo.name()));
        usuariosClient.registrarAccion(accion);
        return TicketRespuestaDTO.desde(ticket, usuariosClient.buscarUsuario(ticket.getRutCliente()));
    }

    /** HU-32: borra el ticket y su conversación. Después no lo ve nadie. */
    @Transactional
    public void eliminarTicket(Long numeroTicket, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_SOPORTE);
        Ticket ticket = findById(numeroTicket);
        ticketRepository.delete(ticket);
        usuariosClient.registrarAccion(accion(rutAdmin, "ELIMINAR",
                "Eliminó el ticket #" + numeroTicket + " (" + ticket.getAsunto() + ") del cliente " + ticket.getRutCliente()));
    }

    /** Para el dashboard de ms-informes: tickets sin resolver (ABIERTO o EN_PROCESO). */
    public long contarAbiertos() {
        return ticketRepository.countByEstadoIn(List.of(EstadoTicket.ABIERTO, EstadoTicket.EN_PROCESO));
    }

    // ---------- Apoyo ----------

    private Ticket findById(Long numeroTicket) {
        return ticketRepository.findById(numeroTicket)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el ticket #" + numeroTicket));
    }

    /** Un cliente solo ve sus tickets; si es de otro, responde como si no existiera. */
    private Ticket buscarDelCliente(Long numeroTicket, String rutCliente) {
        Ticket ticket = findById(numeroTicket);
        if (!ticket.getRutCliente().equals(rutCliente)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el ticket #" + numeroTicket);
        }
        return ticket;
    }

    private void validarNoCerrado(Ticket ticket) {
        if (ticket.getEstado() == EstadoTicket.CERRADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ticket #" + ticket.getNumeroTicket() + " está cerrado y ya no se puede modificar");
        }
    }

    /**
     * Guarda la fecha de resolución cuando el ticket se resuelve o se cierra.
     * Si el administrador lo vuelve a abrir o a EN_PROCESO, la fecha se borra.
     */
    private void cambiarEstado(Ticket ticket, EstadoTicket nuevo) {
        ticket.setEstado(nuevo);
        if (nuevo == EstadoTicket.ABIERTO || nuevo == EstadoTicket.EN_PROCESO) {
            ticket.setFechaResolucion(null);
        } else if (ticket.getFechaResolucion() == null) {
            ticket.setFechaResolucion(LocalDateTime.now());
        }
    }

    private RegistroAccionDTO accion(String rutAdmin, String tipo, String descripcion) {
        return new RegistroAccionDTO(rutAdmin.trim(), tipo, MODULO, descripcion, new ArrayList<>());
    }

    private String obligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
        }
        return valor.trim();
    }

    private String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
