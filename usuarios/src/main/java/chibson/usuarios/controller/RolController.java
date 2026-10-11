package chibson.usuarios.controller;

import chibson.usuarios.model.Permiso;
import chibson.usuarios.model.Rol;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.service.Permisos;
import chibson.usuarios.service.RolService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private static final String RUT = "X-Rut-Usuario";

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    /** Roles con sus permisos (HU-70, HU-71). */
    @GetMapping
    public List<Rol> listarRoles(@RequestHeader(value = RUT, required = false) String rutAdmin) {
        rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return rolService.listarRoles();
    }

    /** Todos los permisos disponibles (HU-71). */
    @GetMapping("/permisos")
    public List<Permiso> listarPermisos(@RequestHeader(value = RUT, required = false) String rutAdmin) {
        rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return rolService.listarPermisos();
    }

    /** Reemplaza los permisos del rol por la lista de ids enviada (HU-71). */
    @PutMapping("/{idRol}/permisos")
    public Rol modificarPermisos(@RequestHeader(value = RUT, required = false) String rutAdmin,
                                 @PathVariable Long idRol, @RequestBody List<Long> idsPermisos) {
        Usuario admin = rolService.exigirPermiso(rutAdmin, Permisos.GESTIONAR_USUARIOS);
        return rolService.modificarPermisos(idRol, idsPermisos, admin.getRut());
    }

    /** Para otros microservicios: ¿este usuario tiene este permiso? (HU-72). */
    @GetMapping("/verificar")
    public boolean tienePermiso(@RequestParam String rut, @RequestParam String permiso) {
        return rolService.tienePermiso(rut, permiso);
    }
}
