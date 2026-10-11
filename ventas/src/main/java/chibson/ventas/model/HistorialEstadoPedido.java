package chibson.ventas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/** Cada cambio de estado del pedido, de solo lectura (HU-36). */
@Entity
@Table(name = "historial_estado_pedido")
@Getter
@Setter
@NoArgsConstructor
public class HistorialEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idHistorial;

    /** Vacío en el registro de creación del pedido. */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private EstadoPedido estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estadoNuevo;

    /** RUT de quien hizo el cambio: el cliente al pagar, el administrador en los demás casos. */
    @Column(nullable = false, length = 12)
    private String rutResponsable;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "numero_pedido", nullable = false)
    private Pedido pedido;
}
