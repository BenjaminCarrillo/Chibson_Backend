package chibson.contenido.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Información de "Quiénes somos" (HU-78).
 * Existe un único registro, siempre con id = 1.
 */
@Entity
@Table(name = "informacion_institucional")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InformacionInstitucional {

    public static final Long ID_UNICO = 1L;

    @Id
    private Long idInformacion;

    @Column(columnDefinition = "TEXT")
    private String mision;

    @Column(columnDefinition = "TEXT")
    private String vision;

    private String correo;

    private String telefono;

    private String direccion;
}
