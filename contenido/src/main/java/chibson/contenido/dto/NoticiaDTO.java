package chibson.contenido.dto;

import chibson.contenido.model.CategoriaNoticia;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Datos que envía el administrador al crear o editar una noticia. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NoticiaDTO {
    private String titulo;
    private String contenido;
    private String imagenUrl;
    private CategoriaNoticia categoria;
    /** Opcional. Si no viene, al publicar se usa la fecha de hoy. */
    private LocalDate fechaPublicacion;
    /** true: se publica de inmediato. false: queda como borrador (HU-76). */
    private boolean publicar;
}
