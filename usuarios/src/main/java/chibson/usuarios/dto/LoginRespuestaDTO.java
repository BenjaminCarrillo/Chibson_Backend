package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lo que recibe el frontend al iniciar sesión (HU-09).
 * Lo guarda mientras dura la sesión y envía el token en el encabezado Authorization: Bearer <token>.
 * El gateway valida el token y, a partir de él, arma el encabezado X-Rut-Usuario para los microservicios.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRespuestaDTO {
    private String rut;
    private String nombres;
    private String primerApellido;
    private String username;
    private String rol;
    private List<String> permisos;
    /** JWT firmado por ms-usuarios. */
    private String token;
    /** Siempre "Bearer": indica cómo se envía el token. */
    private String tipoToken;
    /** Minutos que dura el token antes de expirar. */
    private long expiraEnMinutos;
}
