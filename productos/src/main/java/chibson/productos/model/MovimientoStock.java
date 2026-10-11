package chibson.productos.model;

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

/** Entrada o salida de stock (HU-64). La cantidad es negativa en salidas y positiva en entradas. */
@Entity
@Table(name = "movimiento_stock")
@Getter
@Setter
@NoArgsConstructor
public class MovimientoStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idMovimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoMovimiento tipo;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer saldoResultante;

    /** Pedido que causó el movimiento; vacío en los ajustes manuales. */
    private Long numeroPedido;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_inventario", nullable = false)
    private Inventario inventario;
}
