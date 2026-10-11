package chibson.soporte.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Solicitud de soporte de un cliente (HU-26 a HU-32). */
@Entity
@Table(name = "ticket")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    /** Número de ticket que recibe el cliente como confirmación. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long numeroTicket;

    @Column(nullable = false, length = 12)
    private String rutCliente;

    @Column(nullable = false, length = 150)
    private String asunto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaTicket categoria;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String descripcion;

    /** Imagen o archivo de respaldo, opcional. */
    private String adjuntoUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoTicket estado;

    /** De 1 a 5, la pone el cliente al cerrar (HU-30). */
    private Integer calificacion;

    @Column(length = 500)
    private String comentarioCalificacion;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /** Cuándo pasó a RESUELTO. */
    private LocalDateTime fechaResolucion;

    /** Respuestas del ticket, de la más antigua a la más reciente (HU-27). */
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("fecha ASC")
    private List<MensajeTicket> mensajes = new ArrayList<>();
}
