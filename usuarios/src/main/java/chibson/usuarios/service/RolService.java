package chibson.usuarios.service;

import chibson.usuarios.dto.DetalleCambioDTO;
import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Permiso;
import chibson.usuarios.model.Rol;
import chibson.usuarios.model.TipoAccion;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.repository.PermisoRepository;
import chibson.usuarios.repository.RolRepository;
import chibson.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Roles, permisos y control de acceso (HU-70 a HU-72). */
@Service
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRepository usuarioRepository;
    private final RegistroAccionService registroAccionService;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository,
                      UsuarioRepository usuarioRepository, RegistroAccionService registroAccionService) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.usuarioRepository = usuarioRepository;
        this.registroAccionService = registroAccionService;
    }

    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    public List<Permiso> listarPermisos() {
        return permisoRepository.findAll();
    }

    public Rol buscarRol(String nombre) {
        Validaciones.obligatorio(nombre, "rol");
        return rolRepository.findByNombreIgnoreCase(nombre.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "No existe el rol " + nombre));
    }

    /** Reemplaza los permisos de un rol; aplica de inmediato a todos sus usuarios (HU-71). */
    public Rol modificarPermisos(Long idRol, List<Long> idsPermisos, String rutAdministrador) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el rol " + idRol));
        Set<Permiso> nuevos = new HashSet<>(permisoRepository.findAllById(idsPermisos == null ? List.of() : idsPermisos));
        if (idsPermisos != null && nuevos.size() != new HashSet<>(idsPermisos).size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uno o más permisos no existen");
        }
        String antes = codigos(rol.getPermisos());
        rol.setPermisos(nuevos);
        Rol guardado = rolRepository.save(rol);

        List<DetalleCambioDTO> cambios = RegistroAccionService.sinCambios();
        RegistroAccionService.anotar(cambios, "permisos", antes, codigos(nuevos));
        registroAccionService.registrar(rutAdministrador, TipoAccion.MODIFICAR, "Roles",
                "Modificó los permisos del rol " + rol.getNombre(), cambios);
        return guardado;
    }

    /** Lo usan los demás microservicios para saber si un usuario puede hacer algo (HU-72). */
    public boolean tienePermiso(String rut, String codigoPermiso) {
        if (rut == null || rut.isBlank()) {
            return false;
        }
        return usuarioRepository.findById(rut.trim().toUpperCase())
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .map(u -> u.getRol().getPermisos().stream().anyMatch(p -> p.getCodigo().equals(codigoPermiso)))
                .orElse(false);
    }

    /**
     * Corta la petición si quien la hace no tiene el permiso (HU-72).
     * 401 si no hay sesión o el usuario no existe; 403 si no tiene el permiso.
     */
    public Usuario exigirPermiso(String rut, String codigoPermiso) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        Usuario usuario = usuarioRepository.findById(rut.trim().toUpperCase())
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));
        boolean permitido = usuario.getRol().getPermisos().stream()
                .anyMatch(p -> p.getCodigo().equals(codigoPermiso));
        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tiene permiso para realizar esta acción");
        }
        return usuario;
    }

    /** Exige que haya una sesión válida, sin pedir un permiso en particular. */
    public Usuario exigirSesion(String rut) {
        if (rut == null || rut.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión");
        }
        return usuarioRepository.findById(rut.trim().toUpperCase())
                .filter(u -> u.getEstado() == EstadoUsuario.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Debe iniciar sesión"));
    }

    private String codigos(Set<Permiso> permisos) {
        return permisos.stream().map(Permiso::getCodigo).collect(Collectors.toCollection(TreeSet::new)).toString();
    }
}
