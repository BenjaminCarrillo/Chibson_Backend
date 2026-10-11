package chibson.soporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** Acción de un administrador que se envía a ms-usuarios para el historial (HU-73). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegistroAccionDTO {
    private String rutAdministrador;
    /** CREAR, MODIFICAR, ELIMINAR, ACTIVAR o DESACTIVAR. */
    private String tipo;
    private String modulo;
    private String descripcion;
    private List<DetalleCambioDTO> cambios = new ArrayList<>();
}
