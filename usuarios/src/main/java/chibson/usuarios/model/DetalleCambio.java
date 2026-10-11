package chibson.usuarios.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Campo modificado en una acción, con su valor anterior y nuevo (HU-73). */
@Entity
@Table(name = "detalle_cambio")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idDetalle;

    @Column(nullable = false, length = 50)
    private String campo;

    @Column(columnDefinition = "TEXT")
    private String valorAnterior;

    @Column(columnDefinition = "TEXT")
    private String valorNuevo;
}
