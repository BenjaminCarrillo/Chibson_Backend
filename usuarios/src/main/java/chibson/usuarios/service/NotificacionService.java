package chibson.usuarios.service;

import chibson.usuarios.dto.NotificacionDTO;
import chibson.usuarios.model.Notificacion;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.repository.NotificacionRepository;
import chibson.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

/** Avisos dentro de la plataforma. Reemplazan al correo, que queda fuera del alcance. */
@Service
public class NotificacionService {

    private final NotificacionRepository notificacionRepository;
    private final UsuarioRepository usuarioRepository;

    public NotificacionService(NotificacionRepository notificacionRepository, UsuarioRepository usuarioRepository) {
        this.notificacionRepository = notificacionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public Notificacion enviarNotificacion(NotificacionDTO datos) {
        String rut = Validaciones.normalizarRut(datos.getRutUsuario());
        Usuario usuario = usuarioRepository.findById(rut)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario " + rut));
        Notificacion notificacion = new Notificacion();
        notificacion.setUsuario(usuario);
        notificacion.setMensaje(Validaciones.obligatorio(datos.getMensaje(), "mensaje"));
        notificacion.setReferencia(Validaciones.opcional(datos.getReferencia()));
        notificacion.setLeida(false);
        notificacion.setFecha(LocalDateTime.now());
        return notificacionRepository.save(notificacion);
    }

    public List<Notificacion> listarPorUsuario(String rut) {
        return notificacionRepository.findByUsuarioRutOrderByFechaDesc(rut);
    }

    /** Solo el dueño de la notificación puede marcarla como leída. */
    public Notificacion marcarLeida(Long id, String rutUsuario) {
        Notificacion notificacion = notificacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la notificación " + id));
        if (!notificacion.getUsuario().getRut().equals(rutUsuario)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La notificación no pertenece a este usuario");
        }
        notificacion.setLeida(true);
        return notificacionRepository.save(notificacion);
    }
}
