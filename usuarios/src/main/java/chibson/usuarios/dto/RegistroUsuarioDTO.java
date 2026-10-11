package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Formulario de registro del visitante (HU-08). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroUsuarioDTO {
    private String rut;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String username;
    private String correo;
    private String password;
    private String confirmarPassword;
}
