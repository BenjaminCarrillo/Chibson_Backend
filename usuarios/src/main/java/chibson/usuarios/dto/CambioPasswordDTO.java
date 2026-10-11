package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Cambio de contraseña con sesión iniciada (HU-16). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CambioPasswordDTO {
    private String nuevaPassword;
    private String confirmarPassword;
}
