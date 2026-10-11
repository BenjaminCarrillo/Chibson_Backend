package chibson.usuarios.controller;

import chibson.usuarios.dto.CambioPasswordDTO;
import chibson.usuarios.dto.LoginDTO;
import chibson.usuarios.dto.LoginRespuestaDTO;
import chibson.usuarios.dto.RegistroUsuarioDTO;
import chibson.usuarios.dto.RestablecerPasswordDTO;
import chibson.usuarios.dto.UsuarioDTO;
import chibson.usuarios.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** HU-08 */
    @PostMapping("/registro")
    public ResponseEntity<UsuarioDTO> registrar(@RequestBody RegistroUsuarioDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioDTO.desde(authService.registrar(datos)));
    }

    /** HU-09 */
    @PostMapping("/login")
    public LoginRespuestaDTO login(@RequestBody LoginDTO datos) {
        return authService.login(datos);
    }

    /** HU-10: solicita el enlace. */
    @PostMapping("/recuperar")
    public Map<String, String> solicitarRecuperacion(@RequestParam String correo) {
        authService.solicitarRecuperacion(correo);
        return Map.of("mensaje", "Si el correo está registrado, se envió un enlace de recuperación");
    }

    /** HU-10: usa el enlace para poner una nueva contraseña. */
    @PostMapping("/restablecer")
    public Map<String, String> restablecerPassword(@RequestBody RestablecerPasswordDTO datos) {
        authService.restablecerPassword(datos);
        return Map.of("mensaje", "La contraseña se actualizó correctamente");
    }

    /** HU-16: cambio de contraseña con sesión iniciada. */
    @PutMapping("/password")
    public Map<String, String> cambiarPassword(@RequestHeader(value = "X-Rut-Usuario", required = false) String rut,
                                               @RequestBody CambioPasswordDTO datos) {
        authService.cambiarPassword(rut, datos);
        return Map.of("mensaje", "La contraseña se actualizó correctamente");
    }
}
