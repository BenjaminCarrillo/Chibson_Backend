package chibson.usuarios.dto;

import chibson.usuarios.model.EstadoUsuario;
import chibson.usuarios.model.Usuario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Datos de un usuario que se devuelven al frontend. Nunca incluye la contraseña (HU-11). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private String rut;
    private String nombres;
    private String primerApellido;
    private String segundoApellido;
    private String username;
    private String correo;
    private String telefono;
    private String region;
    private String ciudadComuna;
    private String condominio;
    private String numero;
    private EstadoUsuario estado;
    private LocalDate fechaRegistro;
    private String rol;

    public static UsuarioDTO desde(Usuario u) {
        return new UsuarioDTO(u.getRut(), u.getNombres(), u.getPrimerApellido(), u.getSegundoApellido(),
                u.getUsername(), u.getCorreo(), u.getTelefono(), u.getRegion(), u.getCiudadComuna(),
                u.getCondominio(), u.getNumero(), u.getEstado(), u.getFechaRegistro(),
                u.getRol() == null ? null : u.getRol().getNombre());
    }
}
