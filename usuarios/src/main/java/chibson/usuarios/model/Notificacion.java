package chibson.usuarios.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Aviso dentro de la plataforma: cambios de pedido, respuestas de soporte, devoluciones. */
@Entity
@Table(name = "notificacion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Notificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNotificacion;

    @Column(nullable = false, length = 500)
    private String mensaje;

    /** Qué originó el aviso, por ejemplo "Pedido #12" o "Ticket #3". */
    private String referencia;

    private boolean leida;

    private LocalDateTime fecha;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "rut_usuario", nullable = false)
    private Usuario usuario;
}
