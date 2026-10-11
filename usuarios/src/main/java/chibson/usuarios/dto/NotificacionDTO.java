package chibson.usuarios.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Lo que envían otros microservicios para avisarle algo a un usuario. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificacionDTO {
    private String rutUsuario;
    private String mensaje;
    private String referencia;
}
