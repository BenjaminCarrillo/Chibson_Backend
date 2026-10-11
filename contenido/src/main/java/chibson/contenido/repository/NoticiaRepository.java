package chibson.contenido.repository;

import chibson.contenido.model.EstadoNoticia;
import chibson.contenido.model.Noticia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NoticiaRepository extends JpaRepository<Noticia, Long> {

    /** Noticias públicas, de la más reciente a la más antigua (HU-77). */
    List<Noticia> findByEstadoOrderByFechaPublicacionDescIdNoticiaDesc(EstadoNoticia estado);

    /** Detalle público: solo si la noticia está activada. */
    Optional<Noticia> findByIdNoticiaAndEstado(Long idNoticia, EstadoNoticia estado);

    /** Listado del administrador, de la más nueva a la más antigua. */
    List<Noticia> findAllByOrderByIdNoticiaDesc();
}
