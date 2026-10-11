package chibson.ventas.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Motivo de una cancelación (HU-37) o del rechazo de una devolución (HU-42). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MotivoDTO {
    private String motivo;
}
