package chibson.usuarios.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import chibson.usuarios.dto.PerfilDTO;
import chibson.usuarios.dto.RolDTO;
import chibson.usuarios.dto.UsuarioAdminDTO;
import chibson.usuarios.dto.UsuarioDTO;
import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.service.Permisos;
import chibson.usuarios.service.RolService;
import chibson.usuarios.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private static final String RUT = "X-Rut-Usuario";

    private final UsuarioService usuarioService;
    private final RolService rolService;

    public UsuarioController(UsuarioService usuarioService, RolService rolService) {
        this.usuarioService = usuarioService;
        this.rolService = rolService;
    }

    // ---------- El propio usuario ----------

    /** Datos de "Mi perfil". */
    @GetMapping("/perfil")
    public UsuarioDTO obtenerPerfil(@RequestHeader(value = RUT, required = false) String rut) {
        return UsuarioDTO.desde(rolService.exigirSesion(rut));
    }

    /** HU-12 */
    @PutMapping("/perfil")
    public UsuarioDTO actualizarPerfil(@RequestHeader(value = RUT, required = false) String rut,
                                       @RequestBody PerfilDTO datos) {
        Usuario usuario = rolService.exigirSesion(rut);
        return UsuarioDTO.desde(usuarioService.actualizarPerfil(usuario.getRut(), datos));
    }

    // ---------- Para otros microservicios ----------

    /** Datos públicos de un usuario (nombre y dirección); lo usan ventas y soporte. */
    @GetMapping("/{rut}/datos")
    public UsuarioDTO obtenerDatos(@PathVariable String rut) {
        return UsuarioDTO.desde(usuarioService.findByRut(rut));
    }

    /** Clientes registrados entre dos fechas (AAAA-MM-DD); lo usa ms-informes (HU-50). Sin fechas: hoy. */
    @GetMapping("/registrados/contar")
    public long contarClientesRegistrados(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return usuarioService.contarClientesRegistrados(desde, hasta);
    }

    // ---------- Panel de administración (permiso GESTIONAR_USUARIOS) ----------

    @GetMapping
    public List<UsuarioDTO> listarUsuarios(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                           @RequestParam(required = false) String rol,
                                           @RequestParam(required = false) EstadoUsuario estado,
                                           @RequestParam(required = false) String texto) {
        rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return usuarioService.listarUsuarios(rol, estado, texto).stream().map(UsuarioDTO::desde).toList();
    }

    @GetMapping("/{rut}")
    public UsuarioDTO findByRut(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                @PathVariable String rut) {
        rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return UsuarioDTO.desde(usuarioService.findByRut(rut));
    }

    /** HU-13 */
    @PostMapping
    public ResponseEntity<UsuarioDTO> crearUsuario(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                                   @RequestBody UsuarioAdminDTO datos) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioDTO.desde(usuarioService.crearUsuario(datos, admin.getRut())));
    }

    /** HU-14 */
    @PutMapping("/{rut}")
    public UsuarioDTO editarUsuario(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                    @PathVariable String rut, @RequestBody UsuarioAdminDTO datos) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return UsuarioDTO.desde(usuarioService.editarUsuario(rut, datos, admin.getRut()));
    }

    /** HU-70 */
    @PatchMapping("/{rut}/rol")
    public UsuarioDTO asignarRol(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                 @PathVariable String rut, @RequestBody RolDTO datos) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return UsuarioDTO.desde(usuarioService.asignarRol(rut, datos.getRol(), admin.getRut()));
    }

    /** HU-15 */
    @PatchMapping("/{rut}/desactivar")
    public UsuarioDTO desactivarUsuario(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                        @PathVariable String rut) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return UsuarioDTO.desde(usuarioService.desactivarUsuario(rut, admin.getRut()));
    }

    /** HU-15 */
    @PatchMapping("/{rut}/reactivar")
    public UsuarioDTO reactivarUsuario(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                       @PathVariable String rut) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return UsuarioDTO.desde(usuarioService.reactivarUsuario(rut, admin.getRut()));
    }

    /** HU-17 */
    @DeleteMapping("/{rut}")
    public ResponseEntity<Void> eliminarUsuario(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                                @PathVariable String rut) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        usuarioService.eliminarUsuario(rut, admin.getRut());
        return ResponseEntity.noContent().build();
    }
}
