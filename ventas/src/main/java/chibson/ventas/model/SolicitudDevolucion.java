package chibson.ventas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Solicitud de devolución de uno o varios productos de un pedido entregado (HU-39 a HU-43),
 * y su reembolso una vez aprobada (HU-89).
 */
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

    /** Productos devueltos y cuántas unidades de cada uno. */
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemDevolucion> items = new ArrayList<>();

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

    /** Administrador que aprobó o rechazó. */
    @Column(length = 12)
    private String rutAdministrador;

    @Column(nullable = false)
    private LocalDateTime fechaSolicitud;

    private LocalDateTime fechaResolucion;

    /** Reembolso (HU-89): se llenan al marcarla como reembolsada. */
    private Integer montoReembolsado;

    private LocalDateTime fechaReembolso;

    @Column(length = 12)
    private String rutAdministradorReembolso;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "numero_pedido", nullable = false)
    private Pedido pedido;

    public Long getNumeroPedido() {
        return pedido == null ? null : pedido.getNumeroPedido();
    }

    /** Monto a devolver: unidades devueltas por el precio que el cliente pagó (HU-89 criterio 2). */
    public int calcularMontoReembolso() {
        return items.stream().mapToInt(ItemDevolucion::subtotal).sum();
    }

    /** Marca la solicitud como reembolsada (HU-89 criterio 3). El servicio valida antes que esté aprobada. */
    public void reembolsar(String rutAdmin) {
        this.montoReembolsado = calcularMontoReembolso();
        this.fechaReembolso = LocalDateTime.now();
        this.rutAdministradorReembolso = rutAdmin;
        this.estado = EstadoDevolucion.REEMBOLSADA;
    }
}
