package chibson.usuarios.controller;

import chibson.usuarios.dto.RegistroAccionDTO;
import chibson.usuarios.model.RegistroAccion;
import chibson.usuarios.model.TipoAccion;
import chibson.usuarios.service.Permisos;
import chibson.usuarios.service.RegistroAccionService;
import chibson.usuarios.service.RolService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Historial de acciones (HU-73). No hay endpoints para editar ni borrar: es de solo lectura. */
@RestController
@RequestMapping("/api/acciones")
public class RegistroAccionController {

    private static final String RUT = "X-Rut-Usuario";

    private final RegistroAccionService registroAccionService;
    private final RolService rolService;

    public RegistroAccionController(RegistroAccionService registroAccionService, RolService rolService) {
        this.registroAccionService = registroAccionService;
        this.rolService = rolService;
    }

    /** Para otros microservicios: registra una acción de un administrador. */
    @PostMapping
    public ResponseEntity<RegistroAccion> registrarAccion(@RequestBody RegistroAccionDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(registroAccionService.registrarAccion(datos));
    }

    /** Listado de Historial.html. Fechas en formato AAAA-MM-DD. */
    @GetMapping
    public List<RegistroAccion> listarAcciones(
            @RequestHeader(value = RUT, required = false) String rutAdmin,
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) TipoAccion tipo,
            @RequestParam(required = false) String administrador,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        rolService.exigirPermiso(rutAdmin, Permisos.VER_HISTORIAL);
        return registroAccionService.listarAcciones(texto, tipo, administrador, desde, hasta);
    }

    /** Detalle con campo, valor anterior y valor nuevo. */
    @GetMapping("/{id}")
    public RegistroAccion obtenerDetalle(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                         @PathVariable Long id) {
        rolService.exigirPermiso(rutAdmin, Permisos.VER_HISTORIAL);
        return registroAccionService.obtenerDetalle(id);
    }

    /** Acciones de un administrador específico ("Historial del usuario" en Usuarios.html). */
    @GetMapping("/administrador/{rut}")
    public List<RegistroAccion> listarPorAdministrador(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                                       @PathVariable String rut) {
        rolService.exigirPermiso(rutAdmin, Permisos.VER_HISTORIAL);
        return registroAccionService.listarPorAdministrador(rut);
    }
}
