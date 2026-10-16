package chibson.productos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Producto del catálogo con su stock. Como la tienda tiene una sola sucursal, el stock es un
 * campo del producto; cada cambio queda en el historial de acciones de ms-usuarios (HU-63).
 * Se usa @Getter/@Setter en vez de @Data porque la relación con Resena va en ambos sentidos
 * y @Data generaría un toString/hashCode infinito.
 */
@Entity
@Table(name = "producto")
@Getter
@Setter
@NoArgsConstructor
public class Producto {

    /** Lo genera el sistema al crear el producto, por ejemplo GTR-0001 (HU-01). */
    @Id
    @Column(length = 20)
    private String sku;

    @Column(nullable = false, length = 100)
    private String modelo;

    @Column(nullable = false, length = 60)
    private String marca;

    @Column(columnDefinition = "TEXT")
    private String descripcion;

    @Column(nullable = false)
    private Integer precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoProducto estado;

    @Column(nullable = false)
    private String imagenUrl;

    /** Texto con formato clave:valor;clave:valor (HU-05). */
    @Column(columnDefinition = "TEXT")
    private String fichaTecnica;

    private LocalDateTime fechaModificacion;

    private LocalDateTime fechaModificacionFicha;

    @Column(nullable = false)
    private Integer stockDisponible;

    /** Bajo este número el producto aparece con "Bajo stock" (HU-02). 5 si no se indica. */
    @Column(nullable = false)
    private Integer stockMinimo;

    @JsonIgnore
    @OneToMany(mappedBy = "producto", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Resena> resenas = new ArrayList<>();

    public boolean esBajoStock() {
        return stockDisponible <= stockMinimo;
    }

    public boolean estaAgotado() {
        return stockDisponible == 0;
    }
}
