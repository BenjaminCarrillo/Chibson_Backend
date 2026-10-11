package chibson.soporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos del usuario que entrega ms-usuarios (solo los que soporte necesita). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioDTO {
    private String rut;
    private String nombres;
    private String primerApellido;
    private String estado;

    public String nombreCompleto() {
        return nombres + " " + primerApellido;
    }
}
