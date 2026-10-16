package chibson.usuarios.service;

import chibson.usuarios.client.VentasClient;
import chibson.usuarios.dto.DetalleCambioDTO;
import chibson.usuarios.dto.PerfilDTO;
import chibson.usuarios.dto.UsuarioAdminDTO;
import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Rol;
import chibson.usuarios.model.TipoAccion;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.repository.NotificacionRepository;
import chibson.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Administración de usuarios (HU-12 a HU-15, HU-17, HU-70). */
@Service
public class UsuarioService {

    private static final String MODULO = "Usuarios";

    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;
    private final RolService rolService;
    private final RegistroAccionService registroAccionService;
    private final VentasClient ventasClient;

    public UsuarioService(UsuarioRepository usuarioRepository, NotificacionRepository notificacionRepository,
                          RolService rolService,
                          RegistroAccionService registroAccionService, VentasClient ventasClient) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
        this.rolService = rolService;
        this.registroAccionService = registroAccionService;
        this.ventasClient = ventasClient;
    }

    /** Listado de Usuarios.html con filtros opcionales por rol, estado y texto. */
    public List<Usuario> listarUsuarios(String rol, EstadoUsuario estado, String texto) {
        String busqueda = (texto == null) ? "" : texto.trim().toLowerCase();
        return usuarioRepository.findAllByOrderByFechaRegistroDesc().stream()
                .filter(u -> rol == null || rol.isBlank() || u.getRol().getNombre().equalsIgnoreCase(rol))
                .filter(u -> estado == null || u.getEstado() == estado)
                .filter(u -> busqueda.isEmpty()
                        || u.getRut().toLowerCase().contains(busqueda)
                        || (u.getNombres() + " " + u.getPrimerApellido() + " " + u.getSegundoApellido())
                            .toLowerCase().contains(busqueda)
                        || u.getUsername().toLowerCase().contains(busqueda)
                        || u.getCorreo().toLowerCase().contains(busqueda))
                .toList();
    }

    public Usuario findByRut(String rut) {
        String normalizado = Validaciones.normalizarRut(rut);
        return usuarioRepository.findById(normalizado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el usuario " + normalizado));
    }

    /** El administrador crea una cuenta sin que la persona se registre (HU-13). */
    public Usuario crearUsuario(UsuarioAdminDTO datos, String rutAdministrador) {
        String rut = Validaciones.normalizarRut(datos.getRut());
        if (usuarioRepository.existsById(rut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con el RUT " + rut);
        }
        String correo = Validaciones.validarCorreo(datos.getCorreo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        Validaciones.validarPassword(datos.getPassword(), null);

        Usuario usuario = new Usuario();
        usuario.setRut(rut);
        usuario.setNombres(Validaciones.obligatorio(datos.getNombres(), "nombres"));
        usuario.setPrimerApellido(Validaciones.obligatorio(datos.getPrimerApellido(), "primer apellido"));
        usuario.setSegundoApellido(Validaciones.obligatorio(datos.getSegundoApellido(), "segundo apellido"));
        usuario.setUsername(Validaciones.obligatorio(datos.getUsername(), "nombre de usuario"));
        usuario.setCorreo(correo);
        usuario.setTelefono(Validaciones.obligatorio(datos.getTelefono(), "teléfono"));
        usuario.setPassword(datos.getPassword());
        usuario.setRol(rolService.buscarRol(datos.getRol()));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setFechaRegistro(LocalDate.now());
        Usuario guardado = usuarioRepository.save(usuario);

        registroAccionService.registrar(rutAdministrador, TipoAccion.CREAR, MODULO,
                "Creó el usuario " + guardado.getUsername() + " (" + guardado.getRut() + ")",
                RegistroAccionService.sinCambios());
        return guardado;
    }

    /** El administrador edita los datos de cualquier usuario; el RUT no se puede cambiar (HU-14). */
    public Usuario editarUsuario(String rut, UsuarioAdminDTO datos, String rutAdministrador) {
        Usuario usuario = findByRut(rut);
        String correo = Validaciones.validarCorreo(datos.getCorreo());
        if (!correo.equalsIgnoreCase(usuario.getCorreo()) && usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        String nombres = Validaciones.obligatorio(datos.getNombres(), "nombres");
        String primer = Validaciones.obligatorio(datos.getPrimerApellido(), "primer apellido");
        String segundo = Validaciones.obligatorio(datos.getSegundoApellido(), "segundo apellido");
        String username = Validaciones.obligatorio(datos.getUsername(), "nombre de usuario");
        String telefono = Validaciones.obligatorio(datos.getTelefono(), "teléfono");
        Rol rol = rolService.buscarRol(datos.getRol());

        List<DetalleCambioDTO> cambios = RegistroAccionService.sinCambios();
        RegistroAccionService.anotar(cambios, "nombres", usuario.getNombres(), nombres);
        RegistroAccionService.anotar(cambios, "primerApellido", usuario.getPrimerApellido(), primer);
        RegistroAccionService.anotar(cambios, "segundoApellido", usuario.getSegundoApellido(), segundo);
        RegistroAccionService.anotar(cambios, "username", usuario.getUsername(), username);
        RegistroAccionService.anotar(cambios, "correo", usuario.getCorreo(), correo);
        RegistroAccionService.anotar(cambios, "telefono", usuario.getTelefono(), telefono);
        RegistroAccionService.anotar(cambios, "rol", usuario.getRol().getNombre(), rol.getNombre());

        usuario.setNombres(nombres);
        usuario.setPrimerApellido(primer);
        usuario.setSegundoApellido(segundo);
        usuario.setUsername(username);
        usuario.setCorreo(correo);
        usuario.setTelefono(telefono);
        usuario.setRol(rol);
        Usuario guardado = usuarioRepository.save(usuario);

        registroAccionService.registrar(rutAdministrador, TipoAccion.MODIFICAR, MODULO,
                "Modificó el usuario " + guardado.getUsername() + " (" + guardado.getRut() + ")", cambios);
        return guardado;
    }

    /** Cambia solo el rol de un usuario y deja registro de quién y cuándo (HU-70). */
    public Usuario asignarRol(String rut, String nombreRol, String rutAdministrador) {
        Usuario usuario = findByRut(rut);
        Rol rol = rolService.buscarRol(nombreRol);
        String anterior = usuario.getRol().getNombre();
        usuario.setRol(rol);
        Usuario guardado = usuarioRepository.save(usuario);

        List<DetalleCambioDTO> cambios = RegistroAccionService.sinCambios();
        RegistroAccionService.anotar(cambios, "rol", anterior, rol.getNombre());
        registroAccionService.registrar(rutAdministrador, TipoAccion.MODIFICAR, "Roles",
                "Asignó el rol " + rol.getNombre() + " a " + guardado.getUsername() + " (" + guardado.getRut() + ")", cambios);
        return guardado;
    }

    /** El usuario desactivado no puede iniciar sesión; su historial se conserva (HU-15). */
    public Usuario desactivarUsuario(String rut, String rutAdministrador) {
        Usuario usuario = findByRut(rut);
        if (usuario.getEstado() == EstadoUsuario.DESACTIVADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya está desactivado");
        }
        return cambiarEstado(usuario, EstadoUsuario.DESACTIVADO, TipoAccion.DESACTIVAR, "Desactivó", rutAdministrador);
    }

    public Usuario reactivarUsuario(String rut, String rutAdministrador) {
        Usuario usuario = findByRut(rut);
        if (usuario.getEstado() == EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El usuario ya está activo");
        }
        return cambiarEstado(usuario, EstadoUsuario.ACTIVO, TipoAccion.ACTIVAR, "Reactivó", rutAdministrador);
    }

    /** Borra la cuenta; no se permite si el usuario tiene pedidos (HU-17). */
    @Transactional
    public void eliminarUsuario(String rut, String rutAdministrador) {
        Usuario usuario = findByRut(rut);
        if (ventasClient.clienteTienePedidos(usuario.getRut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El usuario tiene pedidos asociados; desactívelo en lugar de eliminarlo");
        }
        notificacionRepository.deleteByUsuarioRut(usuario.getRut());
        usuarioRepository.delete(usuario);
        registroAccionService.registrar(rutAdministrador, TipoAccion.ELIMINAR, MODULO,
                "Eliminó el usuario " + usuario.getUsername() + " (" + usuario.getRut() + ")",
                RegistroAccionService.sinCambios());
    }

    /** El propio usuario edita sus datos y su dirección predeterminada (HU-12). */
    public Usuario actualizarPerfil(String rut, PerfilDTO datos) {
        Usuario usuario = findByRut(rut);
        usuario.setNombres(Validaciones.obligatorio(datos.getNombres(), "nombres"));
        usuario.setPrimerApellido(Validaciones.obligatorio(datos.getPrimerApellido(), "primer apellido"));
        usuario.setSegundoApellido(Validaciones.obligatorio(datos.getSegundoApellido(), "segundo apellido"));
        usuario.setUsername(Validaciones.obligatorio(datos.getUsername(), "nombre de usuario"));
        usuario.setTelefono(Validaciones.opcional(datos.getTelefono()));
        usuario.setRegion(Validaciones.obligatorio(datos.getRegion(), "región"));
        usuario.setCiudadComuna(Validaciones.obligatorio(datos.getCiudadComuna(), "ciudad o comuna"));
        usuario.setCondominio(Validaciones.opcional(datos.getCondominio()));
        usuario.setNumero(Validaciones.obligatorio(datos.getNumero(), "número"));
        return usuarioRepository.save(usuario);
    }

    /**
     * Clientes registrados entre dos fechas (ambas incluidas); lo usa ms-informes para el resumen
     * general (HU-50). Sin fechas cuenta los de hoy.
     */
    public long contarClientesRegistrados(LocalDate desde, LocalDate hasta) {
        LocalDate inicio = desde == null ? LocalDate.now() : desde;
        LocalDate fin = hasta == null ? inicio : hasta;
        if (inicio.isAfter(fin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }
        return usuarioRepository.countByFechaRegistroBetweenAndRolNombre(inicio, fin, Rol.CLIENTE);
    }

    private Usuario cambiarEstado(Usuario usuario, EstadoUsuario nuevo, TipoAccion tipo, String verbo,
                                  String rutAdministrador) {
        EstadoUsuario anterior = usuario.getEstado();
        usuario.setEstado(nuevo);
        Usuario guardado = usuarioRepository.save(usuario);
        List<DetalleCambioDTO> cambios = RegistroAccionService.sinCambios();
        RegistroAccionService.anotar(cambios, "estado", anterior, nuevo);
        registroAccionService.registrar(rutAdministrador, tipo, MODULO,
                verbo + " el usuario " + guardado.getUsername() + " (" + guardado.getRut() + ")", cambios);
        return guardado;
    }
}
