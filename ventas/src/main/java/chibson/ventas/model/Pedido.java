package chibson.ventas.model;

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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Pedido de compra. Una "venta" es un pedido con pago aprobado (HU-57).
 * Los campos de envío solo se llenan si el cliente ingresó una dirección distinta a la de su
 * perfil para este pedido (HU-25); si están vacíos, se usa la dirección del perfil.
 */
@Entity
@Table(name = "pedido")
@Getter
@Setter
@NoArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long numeroPedido;

    @Column(nullable = false, length = 12)
    private String rutCliente;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoPedido estado;

    @Column(nullable = false)
    private Integer subtotal;

    @Column(nullable = false)
    private Integer costoEnvio;

    /** IVA incluido en el subtotal (informativo). */
    @Column(nullable = false)
    private Integer iva;

    @Column(nullable = false)
    private Integer total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MetodoPago metodoPago;

    private String motivoCancelacion;

    private String regionEnvio;
    private String ciudadComunaEnvio;
    private String condominioEnvio;
    private String numeroEnvio;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<DetallePedido> detalles = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<HistorialEstadoPedido> historial = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha ASC")
    private List<Pago> pagos = new ArrayList<>();

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SolicitudDevolucion> devoluciones = new ArrayList<>();

    public boolean usaDireccionAlternativa() {
        return regionEnvio != null;
    }

    /** Transiciones que puede hacer el administrador (HU-35). Pagar y cancelar tienen su propia acción. */
    public boolean puedeCambiarA(EstadoPedido nuevo) {
        return (estado == EstadoPedido.PAGADO && nuevo == EstadoPedido.EN_PROCESO)
                || (estado == EstadoPedido.EN_PROCESO && nuevo == EstadoPedido.ENVIADO)
                || (estado == EstadoPedido.ENVIADO && nuevo == EstadoPedido.ENTREGADO);
    }

    /** "Facturado" = tiene un pago aprobado (HU-44). */
    public boolean tienePagoAprobado() {
        return pagos.stream().anyMatch(p -> p.getEstado() == EstadoPago.APROBADO);
    }
}
