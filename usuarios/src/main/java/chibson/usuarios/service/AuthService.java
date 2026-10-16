package chibson.usuarios.service;

import chibson.usuarios.dto.CambioPasswordDTO;
import chibson.usuarios.dto.LoginDTO;
import chibson.usuarios.dto.LoginRespuestaDTO;
import chibson.usuarios.dto.RegistroUsuarioDTO;
import chibson.usuarios.dto.RecuperarPasswordDTO;
import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Permiso;
import chibson.usuarios.model.Rol;
import chibson.usuarios.model.Usuario;
import chibson.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

/** Registro, inicio de sesión y contraseñas (HU-08 a HU-10, HU-16). */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final RolService rolService;

    public AuthService(UsuarioRepository usuarioRepository, RolService rolService) {
        this.usuarioRepository = usuarioRepository;
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
     * Recuperación de contraseña sin enlace ni token (HU-10): si el RUT y el correo son de la misma
     * cuenta, Usuario.recuperarPassword() pone la contraseña nueva. Si no coinciden, el mensaje no
     * dice cuál de los dos está mal, para no revelar qué cuentas existen.
     */
    public void recuperarPassword(RecuperarPasswordDTO datos) {
        Validaciones.obligatorio(datos.getCorreo(), "correo");
        if (datos.getConfirmarPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe confirmar la contraseña");
        }
        Validaciones.validarPassword(datos.getNuevaPassword(), datos.getConfirmarPassword());
        String rut = Validaciones.normalizarRut(datos.getRut());
        Usuario usuario = usuarioRepository.findById(rut).orElse(null);
        if (usuario == null || !usuario.recuperarPassword(datos.getCorreo(), datos.getNuevaPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El RUT y el correo no corresponden a una misma cuenta");
        }
        if (usuario.getEstado() == EstadoUsuario.DESACTIVADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está desactivada");
        }
        usuarioRepository.save(usuario);
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
