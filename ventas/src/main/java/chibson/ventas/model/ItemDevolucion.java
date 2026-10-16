package chibson.ventas.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Producto incluido en una solicitud de devolución, con las unidades que se devuelven (HU-39).
 * Guarda el precio unitario que el cliente pagó en el pedido para calcular el reembolso (HU-89).
 */
@Entity
@Table(name = "item_devolucion")
@Getter
@Setter
@NoArgsConstructor
public class ItemDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItemDevolucion;

    @Column(nullable = false, length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String nombreProducto;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer precioUnitario;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "numero_seguimiento", nullable = false)
    private SolicitudDevolucion solicitud;

    @JsonProperty("subtotal")
    public int subtotal() {
        return cantidad * precioUnitario;
    }
}
