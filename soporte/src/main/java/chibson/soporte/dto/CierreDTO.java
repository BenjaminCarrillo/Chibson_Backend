package chibson.soporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** El cliente cierra un ticket resuelto y, si quiere, califica la atención (HU-30). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CierreDTO {
    /** Opcional, de 1 a 5. */
    private Integer calificacion;
    /** Opcional. */
    private String comentario;
}
