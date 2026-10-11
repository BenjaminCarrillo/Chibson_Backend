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

/** Solicitud de devolución de un producto de un pedido entregado (HU-39 a HU-43). */
@Entity
@Table(name = "solicitud_devolucion")
@Getter
@Setter
@NoArgsConstructor
public class SolicitudDevolucion {

    /** Número de seguimiento que recibe el cliente. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long numeroSeguimiento;

    @Column(nullable = false, length = 12)
    private String rutCliente;

    @Column(nullable = false, length = 20)
    private String sku;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MotivoDevolucion motivo;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    /** Imagen opcional del producto. */
    private String evidenciaUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoDevolucion estado;

    private String motivoRechazo;

    @Column(length = 12)
    private String rutAdministrador;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaResolucion;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "numero_pedido", nullable = false)
    private Pedido pedido;

    public Long getNumeroPedido() {
        return pedido == null ? null : pedido.getNumeroPedido();
    }
}
