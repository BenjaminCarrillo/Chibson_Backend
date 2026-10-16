package chibson.soporte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** Acción de un administrador que se envía a ms-usuarios para el historial (HU-73). */
@Data
@AllArgsConstructor
public class RegistroAccionDTO {
    private String rutAdministrador;
    /** CREAR, MODIFICAR, ELIMINAR, ACTIVAR o DESACTIVAR. */
    private String tipo;
    private String modulo;
    private String descripcion;
    private List<DetalleCambioDTO> cambios;

    /**
     * Constructor vacío (lo usan JPA y Jackson). La lista se inicializa aquí y no en la declaración
     * del campo: con Java 25, el compilador de VS Code marca error si un campo tiene inicializador y
     * además lo asigna el constructor con todos los argumentos que genera @AllArgsConstructor.
     */
    public RegistroAccionDTO() {
        this.cambios = new ArrayList<>();
    }
}
