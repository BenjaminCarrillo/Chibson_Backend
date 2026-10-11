package chibson.productos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Stock de un producto (EP7). Es el único lugar donde se guardan las existencias. */
@Entity
@Table(name = "inventario")
@Getter
@Setter
@NoArgsConstructor
public class Inventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idInventario;

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "sku", nullable = false, unique = true)
    private Producto producto;

    @Column(nullable = false)
    private Integer stockDisponible;

    @Column(nullable = false)
    private Integer stockMinimo;

    private LocalDateTime fechaActualizacion;

    @JsonIgnore
    @OneToMany(mappedBy = "inventario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovimientoStock> movimientos = new ArrayList<>();

    /** Stock igual o menor al mínimo (HU-02). */
    public boolean esBajoStock() {
        return stockDisponible <= stockMinimo;
    }
}
