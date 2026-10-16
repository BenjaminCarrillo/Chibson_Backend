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
 * Producto comprado dentro de un pedido, con el precio que tenía al comprarlo.
 * Llega directo desde el frontend al pagar: ya no hay carrito en el backend.
 */
@Entity
@Table(name = "item_pedido")
@Getter
@Setter
@NoArgsConstructor
public class ItemPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idItem;

    @Column(nullable = false, length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String nombreProducto;

    @Column(length = 20)
    private String categoria;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer precioUnitario;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "numero_pedido", nullable = false)
    private Pedido pedido;

    /** Se incluye en el JSON para mostrarlo en el comprobante. */
    @JsonProperty("subtotal")
    public int subtotal() {
        return cantidad * precioUnitario;
    }
}
