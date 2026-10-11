package chibson.usuarios.service;

import chibson.usuarios.dto.CambioPasswordDTO;
import chibson.usuarios.dto.LoginDTO;
import chibson.usuarios.dto.LoginRespuestaDTO;
import chibson.usuarios.dto.RegistroUsuarioDTO;
import chibson.usuarios.dto.RestablecerPasswordDTO;
import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Permiso;
import chibson.usuarios.model.Rol;
import chibson.usuarios.model.TokenRecuperacion;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.repository.TokenRecuperacionRepository;
import chibson.usuarios.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Registro, inicio de sesión y contraseñas (HU-08 a HU-10, HU-16). */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final int MINUTOS_VIGENCIA_TOKEN = 30;

    private final UsuarioRepository usuarioRepository;
    private final TokenRecuperacionRepository tokenRepository;
    private final RolService rolService;

    public AuthService(UsuarioRepository usuarioRepository, TokenRecuperacionRepository tokenRepository,
                       RolService rolService) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.rolService = rolService;
    }

    /** El visitante crea su cuenta; queda como CLIENTE (HU-08). */
    public Usuario registrar(RegistroUsuarioDTO datos) {
        String rut = Validaciones.normalizarRut(datos.getRut());
        if (usuarioRepository.existsById(rut)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una cuenta con ese RUT");
        }
        String correo = Validaciones.validarCorreo(datos.getCorreo());
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya está registrado");
        }
        if (datos.getConfirmarPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe confirmar la contraseña");
        }
        Validaciones.validarPassword(datos.getPassword(), datos.getConfirmarPassword());

        Usuario usuario = new Usuario();
        usuario.setRut(rut);
        usuario.setNombres(Validaciones.obligatorio(datos.getNombres(), "nombres"));
        usuario.setPrimerApellido(Validaciones.obligatorio(datos.getPrimerApellido(), "primer apellido"));
        usuario.setSegundoApellido(Validaciones.obligatorio(datos.getSegundoApellido(), "segundo apellido"));
        usuario.setUsername(Validaciones.obligatorio(datos.getUsername(), "nombre de usuario"));
        usuario.setCorreo(correo);
        usuario.setPassword(datos.getPassword());
        usuario.setRol(rolService.buscarRol(Rol.CLIENTE));
        usuario.setEstado(EstadoUsuario.ACTIVO);
        usuario.setFechaRegistro(LocalDate.now());
        return usuarioRepository.save(usuario);
    }

    /**
     * Valida correo y contraseña (HU-09). Si fallan, el mensaje no dice cuál de los dos estaba mal.
     * Devuelve el rol para que el frontend redirija al panel que corresponde.
     */
    public LoginRespuestaDTO login(LoginDTO datos) {
        ResponseStatusException credencialesInvalidas =
                new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Correo o contraseña incorrectos");
        if (datos.getCorreo() == null || datos.getPassword() == null) {
            throw credencialesInvalidas;
        }
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(datos.getCorreo().trim())
                .orElseThrow(() -> credencialesInvalidas);
        if (!usuario.getPassword().equals(datos.getPassword())) {
            throw credencialesInvalidas;
        }
        if (usuario.getEstado() == EstadoUsuario.DESACTIVADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está desactivada");
        }
        List<String> permisos = usuario.getRol().getPermisos().stream().map(Permiso::getCodigo).sorted().toList();
        return new LoginRespuestaDTO(usuario.getRut(), usuario.getNombres(), usuario.getPrimerApellido(),
                usuario.getUsername(), usuario.getRol().getNombre(), permisos);
    }

    /**
     * Genera un enlace de recuperación válido por 30 minutos (HU-10).
     * El envío por correo está simulado: el enlace se muestra en la consola del microservicio.
     * La respuesta es la misma exista o no el correo, para no revelar qué correos están registrados.
     */
    public void solicitarRecuperacion(String correo) {
        if (correo == null || correo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El correo es obligatorio");
        }
        Optional<Usuario> usuario = usuarioRepository.findByCorreoIgnoreCase(correo.trim());
        if (usuario.isEmpty()) {
            return;
        }
        TokenRecuperacion token = new TokenRecuperacion();
        token.setToken(UUID.randomUUID().toString());
        token.setExpiraEn(LocalDateTime.now().plusMinutes(MINUTOS_VIGENCIA_TOKEN));
        token.setUsado(false);
        token.setUsuario(usuario.get());
        tokenRepository.save(token);
        log.info("[Correo simulado] Enlace de recuperación para {}: token={}", usuario.get().getCorreo(), token.getToken());
    }

    /** Establece la nueva contraseña con un enlace vigente y sin usar (HU-10). */
    public void restablecerPassword(RestablecerPasswordDTO datos) {
        TokenRecuperacion token = tokenRepository.findByToken(Validaciones.obligatorio(datos.getToken(), "token"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace no es válido"));
        if (token.isUsado()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace ya fue utilizado");
        }
        if (token.getExpiraEn().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El enlace venció; solicite uno nuevo");
        }
        Validaciones.validarPassword(datos.getNuevaPassword(), datos.getConfirmarPassword());
        Usuario usuario = token.getUsuario();
        usuario.setPassword(datos.getNuevaPassword());
        usuarioRepository.save(usuario);
        token.setUsado(true);
        tokenRepository.save(token);
    }

    /** Cambio de contraseña con la sesión iniciada (HU-16). */
    public void cambiarPassword(String rut, CambioPasswordDTO datos) {
        Usuario usuario = rolService.exigirSesion(rut);
        if (datos.getConfirmarPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe confirmar la contraseña");
        }
        Validaciones.validarPassword(datos.getNuevaPassword(), datos.getConfirmarPassword());
        usuario.setPassword(datos.getNuevaPassword());
        usuarioRepository.save(usuario);
    }
}
