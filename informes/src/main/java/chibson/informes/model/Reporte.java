package chibson.informes.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte generado y guardado (HU-45 a HU-50). Se guardan los datos tal como estaban al generarlo,
 * así que no cambia aunque después cambien las ventas o los pedidos (HU-61).
 */
@Entity
@Table(name = "reporte")
@Getter
@Setter
@NoArgsConstructor
public class Reporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idReporte;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoReporte tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Periodo periodo;

    @Column(nullable = false)
    private LocalDate fechaDesde;

    @Column(nullable = false)
    private LocalDate fechaHasta;

    /** Solo en productos más vendidos, si se filtró por categoría. */
    @Column(length = 20)
    private String categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrigenReporte origen;

    /** Administrador que lo generó; vacío en los automáticos. */
    @Column(length = 12)
    private String rutAdministrador;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    /** Ventas netas de reembolsos del periodo. */
    private Integer totalVentas;

    private Long numeroPedidos;

    private Integer ticketPromedio;

    /** Aviso cuando el periodo no tiene datos, para no mostrar un reporte vacío. */
    @Column(columnDefinition = "TEXT")
    private String mensaje;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orden ASC")
    private List<FilaReporte> filas = new ArrayList<>();

    /** Agrega una fila al final, numerándola en orden. */
    public FilaReporte agregarFila(String seccion, String etiqueta, Long cantidad, Integer monto) {
        FilaReporte fila = new FilaReporte();
        fila.setOrden(filas.size() + 1);
        fila.setSeccion(seccion);
        fila.setEtiqueta(etiqueta);
        fila.setCantidad(cantidad);
        fila.setMonto(monto);
        fila.setReporte(this);
        filas.add(fila);
        return fila;
    }
}
