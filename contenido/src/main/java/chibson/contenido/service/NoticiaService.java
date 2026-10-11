package chibson.contenido.service;

import chibson.contenido.client.UsuariosClient;
import chibson.contenido.dto.DetalleCambioDTO;
import chibson.contenido.dto.NoticiaDTO;
import chibson.contenido.dto.RegistroAccionDTO;
import chibson.contenido.model.CategoriaNoticia;
import chibson.contenido.model.EstadoNoticia;
import chibson.contenido.model.Noticia;
import chibson.contenido.repository.NoticiaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class NoticiaService {

    private static final String MODULO = "Noticias";

    private final NoticiaRepository noticiaRepository;
    private final UsuariosClient usuariosClient;

    public NoticiaService(NoticiaRepository noticiaRepository, UsuariosClient usuariosClient) {
        this.noticiaRepository = noticiaRepository;
        this.usuariosClient = usuariosClient;
    }

    // ---------- Vista pública (HU-77) ----------

    /** Noticias activadas, de la más reciente a la más antigua. */
    public List<Noticia> listarPublicadas() {
        return noticiaRepository.findByEstadoOrderByFechaPublicacionDescIdNoticiaDesc(EstadoNoticia.ACTIVADA);
    }

    /** Contenido completo de una noticia; las borradas, borradores o desactivadas no son accesibles (HU-75, HU-79). */
    public Noticia obtenerPublicada(Long id) {
        return noticiaRepository.findByIdNoticiaAndEstado(id, EstadoNoticia.ACTIVADA)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La noticia no existe o no está publicada"));
    }

    // ---------- Panel de administración ----------

    /** Listado con filtros combinables por categoría, estado y título (HU-80). Todos son opcionales. */
    public List<Noticia> listarNoticias(CategoriaNoticia categoria, EstadoNoticia estado, String titulo) {
        String texto = (titulo == null) ? "" : titulo.trim().toLowerCase();
        return noticiaRepository.findAllByOrderByIdNoticiaDesc().stream()
                .filter(n -> categoria == null || n.getCategoria() == categoria)
                .filter(n -> estado == null || n.getEstado() == estado)
                .filter(n -> texto.isEmpty() || n.getTitulo().toLowerCase().contains(texto))
                .toList();
    }

    public Noticia findById(Long id) {
        return noticiaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la noticia " + id));
    }

    /** Publica de inmediato o guarda como borrador (HU-76, HU-80). */
    public Noticia crearNoticia(NoticiaDTO datos, String rutAdministrador) {
        validar(datos);
        Noticia noticia = new Noticia();
        noticia.setTitulo(datos.getTitulo().trim());
        noticia.setContenido(datos.getContenido().trim());
        noticia.setImagenUrl(vacioANull(datos.getImagenUrl()));
        noticia.setCategoria(datos.getCategoria());
        if (datos.isPublicar()) {
            noticia.setEstado(EstadoNoticia.ACTIVADA);
            noticia.setFechaPublicacion(datos.getFechaPublicacion() != null ? datos.getFechaPublicacion() : LocalDate.now());
        } else {
            noticia.setEstado(EstadoNoticia.BORRADOR);
            noticia.setFechaPublicacion(datos.getFechaPublicacion());
        }
        noticia.setFechaModificacion(LocalDateTime.now());
        Noticia guardada = noticiaRepository.save(noticia);

        String accion = datos.isPublicar() ? "Publicó" : "Guardó como borrador";
        registrar(rutAdministrador, "CREAR", accion + " la noticia \"" + guardada.getTitulo() + "\"", new ArrayList<>());
        return guardada;
    }

    /** Modifica título, contenido, imagen, categoría o fecha de publicación (HU-74). */
    public Noticia editarNoticia(Long id, NoticiaDTO datos, String rutAdministrador) {
        validar(datos);
        Noticia noticia = findById(id);
        List<DetalleCambioDTO> cambios = new ArrayList<>();

        String titulo = datos.getTitulo().trim();
        String contenido = datos.getContenido().trim();
        String imagen = vacioANull(datos.getImagenUrl());

        anotar(cambios, "titulo", noticia.getTitulo(), titulo);
        anotar(cambios, "contenido", noticia.getContenido(), contenido);
        anotar(cambios, "imagenUrl", noticia.getImagenUrl(), imagen);
        anotar(cambios, "categoria", noticia.getCategoria(), datos.getCategoria());

        noticia.setTitulo(titulo);
        noticia.setContenido(contenido);
        noticia.setImagenUrl(imagen);
        noticia.setCategoria(datos.getCategoria());
        if (datos.getFechaPublicacion() != null) {
            anotar(cambios, "fechaPublicacion", noticia.getFechaPublicacion(), datos.getFechaPublicacion());
            noticia.setFechaPublicacion(datos.getFechaPublicacion());
        }
        noticia.setFechaModificacion(LocalDateTime.now());
        Noticia guardada = noticiaRepository.save(noticia);

        registrar(rutAdministrador, "MODIFICAR", "Modificó la noticia \"" + guardada.getTitulo() + "\"", cambios);
        return guardada;
    }

    /** Publica un borrador o vuelve a mostrar una noticia desactivada (HU-79). */
    public Noticia activarNoticia(Long id, String rutAdministrador) {
        Noticia noticia = findById(id);
        if (noticia.getEstado() == EstadoNoticia.ACTIVADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La noticia ya está activada");
        }
        EstadoNoticia anterior = noticia.getEstado();
        noticia.setEstado(EstadoNoticia.ACTIVADA);
        if (noticia.getFechaPublicacion() == null) {
            noticia.setFechaPublicacion(LocalDate.now());
        }
        noticia.setFechaModificacion(LocalDateTime.now());
        Noticia guardada = noticiaRepository.save(noticia);

        List<DetalleCambioDTO> cambios = new ArrayList<>();
        anotar(cambios, "estado", anterior, EstadoNoticia.ACTIVADA);
        registrar(rutAdministrador, "ACTIVAR", "Activó la noticia \"" + guardada.getTitulo() + "\"", cambios);
        return guardada;
    }

    /** Oculta la noticia del sitio público sin eliminarla (HU-79). */
    public Noticia desactivarNoticia(Long id, String rutAdministrador) {
        Noticia noticia = findById(id);
        if (noticia.getEstado() != EstadoNoticia.ACTIVADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se puede desactivar una noticia activada");
        }
        noticia.setEstado(EstadoNoticia.DESACTIVADA);
        noticia.setFechaModificacion(LocalDateTime.now());
        Noticia guardada = noticiaRepository.save(noticia);

        List<DetalleCambioDTO> cambios = new ArrayList<>();
        anotar(cambios, "estado", EstadoNoticia.ACTIVADA, EstadoNoticia.DESACTIVADA);
        registrar(rutAdministrador, "DESACTIVAR", "Desactivó la noticia \"" + guardada.getTitulo() + "\"", cambios);
        return guardada;
    }

    /** Elimina la noticia definitivamente y lo deja en el historial (HU-75). */
    public void eliminarNoticia(Long id, String rutAdministrador) {
        Noticia noticia = findById(id);
        noticiaRepository.delete(noticia);
        registrar(rutAdministrador, "ELIMINAR", "Eliminó la noticia \"" + noticia.getTitulo() + "\"", new ArrayList<>());
    }

    // ---------- Apoyo ----------

    private void validar(NoticiaDTO datos) {
        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio");
        }
        if (datos.getTitulo().trim().length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título no puede superar los 150 caracteres");
        }
        if (datos.getContenido() == null || datos.getContenido().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El contenido es obligatorio");
        }
        if (datos.getCategoria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría es obligatoria");
        }
    }

    private String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private void anotar(List<DetalleCambioDTO> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new DetalleCambioDTO(campo,
                    anterior == null ? null : anterior.toString(),
                    nuevo == null ? null : nuevo.toString()));
        }
    }

    private void registrar(String rut, String tipo, String descripcion, List<DetalleCambioDTO> cambios) {
        usuariosClient.registrarAccion(new RegistroAccionDTO(rut, tipo, MODULO, descripcion, cambios));
    }
}
