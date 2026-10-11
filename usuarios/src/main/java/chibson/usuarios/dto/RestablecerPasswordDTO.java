package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Nueva contraseña usando el enlace de recuperación (HU-10). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestablecerPasswordDTO {
    private String token;
    private String nuevaPassword;
    private String confirmarPassword;
}
