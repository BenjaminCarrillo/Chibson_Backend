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

/** Reseña de un cliente sobre un producto que compró y recibió (EP11). */
@Entity
@Table(name = "resena")
@Getter
@Setter
@NoArgsConstructor
public class Resena {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idResena;

    @Column(nullable = false, length = 12)
    private String rutCliente;

    /** Nombre que se muestra junto a la reseña (HU-82). Se toma de ms-usuarios al crearla. */
    @Column(nullable = false, length = 120)
    private String nombreCliente;

    @Column(nullable = false)
    private Integer calificacion;

    @Column(nullable = false, length = 100)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoResena estado;

    /** Respuesta pública del administrador; solo una por reseña (HU-87). */
    @Column(columnDefinition = "TEXT")
    private String respuestaAdmin;

    @Column(nullable = false)
    private LocalDateTime fecha;

    private LocalDateTime fechaRespuesta;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "sku", nullable = false)
    private Producto producto;

    /** Para que el JSON muestre a qué producto pertenece la reseña. */
    public String getSku() {
        return producto == null ? null : producto.getSku();
    }

    public String getModeloProducto() {
        return producto == null ? null : producto.getModelo();
    }
}
