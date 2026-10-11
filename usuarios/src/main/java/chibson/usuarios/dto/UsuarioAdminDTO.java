package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos que envía el administrador al crear (HU-13) o editar (HU-14) un usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAdminDTO {
    /** Solo se usa al crear; al editar se ignora porque el RUT no se puede cambiar. */
    private String rut;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String username;
    private String correo;
    private String telefono;
    /** ADMINISTRADOR o CLIENTE. */
    private String rol;
    /** Solo se usa al crear: contraseña inicial del usuario. */
    private String password;
}
