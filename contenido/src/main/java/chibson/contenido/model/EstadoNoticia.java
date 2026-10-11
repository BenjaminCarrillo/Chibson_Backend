package chibson.contenido.model;

/**
 * Estados de una noticia.
 * BORRADOR: guardada sin publicar (HU-76).
 * ACTIVADA: visible en el sitio público.
 * DESACTIVADA: oculta temporalmente sin eliminarla (HU-79).
 */
public enum EstadoNoticia {
    BORRADOR,
    ACTIVADA,
    DESACTIVADA
}
