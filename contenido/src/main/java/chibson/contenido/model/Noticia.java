package chibson.contenido.model;

import chibson.contenido.service.ImagenService;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "noticia")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNoticia;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String contenido;

    /**
     * Opcional (HU-76). Nombre del archivo de la imagen dentro de la carpeta de imágenes de
     * ms-contenido (por ejemplo "carrusel1.jpg"). No se guarda la ruta completa.
     */
    @Column(length = 80)
    private String imagen;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategoriaNoticia categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoNoticia estado;

    /** Se llena al publicar; queda vacía mientras es borrador. */
    private LocalDate fechaPublicacion;

    /** Fecha de la última modificación (HU-74). */
    private LocalDateTime fechaModificacion;

    /** Dirección para mostrar la imagen (/imagenes/noticias/<imagen>). Se arma, no se guarda en la base. */
    public String getImagenUrl() {
        return ImagenService.urlPublica(imagen);
    }
}
