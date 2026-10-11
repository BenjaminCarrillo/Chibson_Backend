package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Lo que recibe el frontend al iniciar sesión (HU-09).
 * Lo guarda mientras dura la sesión y envía el RUT en el encabezado X-Rut-Usuario.
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
}
