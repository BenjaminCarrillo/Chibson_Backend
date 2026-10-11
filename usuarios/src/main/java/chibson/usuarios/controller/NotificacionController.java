package chibson.usuarios.controller;

import chibson.usuarios.dto.NotificacionDTO;
import chibson.usuarios.model.Notificacion;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.service.NotificacionService;
import chibson.usuarios.service.RolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private static final String RUT = "X-Rut-Usuario";

    private final NotificacionService notificacionService;
    private final RolService rolService;

    public NotificacionController(NotificacionService notificacionService, RolService rolService) {
        this.notificacionService = notificacionService;
        this.rolService = rolService;
    }

    /** Para otros microservicios: crea un aviso para un usuario. */
    @PostMapping
    public ResponseEntity<Notificacion> enviarNotificacion(@RequestBody NotificacionDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(notificacionService.enviarNotificacion(datos));
    }

    /** Avisos del usuario que inició sesión, del más nuevo al más antiguo. */
    @GetMapping
    public List<Notificacion> listarPorUsuario(@RequestHeader(value = RUT, required = false) String rut) {
        Usuario usuario = rolService.exigirSesion(rut);
        return notificacionService.listarPorUsuario(usuario.getRut());
    }

    @PatchMapping("/{id}/leida")
    public Notificacion marcarLeida(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long id) {
        Usuario usuario = rolService.exigirSesion(rut);
        return notificacionService.marcarLeida(id, usuario.getRut());
    }
}
