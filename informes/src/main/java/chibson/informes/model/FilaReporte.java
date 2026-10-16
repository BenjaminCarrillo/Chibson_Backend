package chibson.informes.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Una fila de un reporte. La sección agrupa las filas (por ejemplo "TOP_5" o "POR_CATEGORIA" en el
 * resumen general, o el estado en el reporte de pedidos).
 */
@Entity
@Table(name = "fila_reporte")
@Getter
@Setter
@NoArgsConstructor
public class FilaReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idFila;

    @Column(nullable = false)
    private Integer orden;

    @Column(nullable = false, length = 40)
    private String seccion;

    /** Lo que describe la fila: una fecha, un producto, una categoría, un estado, un indicador... */
    @Column(nullable = false, length = 150)
    private String etiqueta;

    /** Texto para indicadores que no son un número, por ejemplo "Sin ventas en el periodo anterior". */
    @Column(length = 150)
    private String valor;

    private Long cantidad;

    private Integer monto;

    /** Para abrir el detalle desde la fila: el número de pedido (HU-46) o el SKU del producto. */
    @Column(length = 30)
    private String referencia;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_reporte", nullable = false)
    private Reporte reporte;
}
