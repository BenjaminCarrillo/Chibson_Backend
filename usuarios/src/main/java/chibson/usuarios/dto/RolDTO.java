package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Rol que se asigna a un usuario (HU-70). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolDTO {
    /** ADMINISTRADOR o CLIENTE. */
    private String rol;
}
