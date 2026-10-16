package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Recuperación de contraseña (HU-10): el RUT y el correo deben ser de la misma cuenta. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecuperarPasswordDTO {
    private String rut;
    private String correo;
    private String nuevaPassword;
    private String confirmarPassword;
}
