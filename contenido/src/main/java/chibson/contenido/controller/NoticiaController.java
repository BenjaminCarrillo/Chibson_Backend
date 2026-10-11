package chibson.contenido.controller;

import chibson.contenido.dto.NoticiaDTO;
import chibson.contenido.model.CategoriaNoticia;
import chibson.contenido.model.EstadoNoticia;
import chibson.contenido.model.Noticia;
import chibson.contenido.service.NoticiaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/noticias")
public class NoticiaController {

    /** Encabezado con el RUT del usuario que hace la petición. */
    private static final String RUT = "X-Rut-Usuario";

    private final NoticiaService noticiaService;

    public NoticiaController(NoticiaService noticiaService) {
        this.noticiaService = noticiaService;
    }

    // ---------- Vista pública ----------

    @GetMapping("/publicadas")
    public List<Noticia> listarPublicadas() {
        return noticiaService.listarPublicadas();
    }

    @GetMapping("/publicadas/{id}")
    public Noticia obtenerPublicada(@PathVariable Long id) {
        return noticiaService.obtenerPublicada(id);
    }

    // ---------- Panel de administración ----------

    @GetMapping
    public List<Noticia> listarNoticias(@RequestParam(required = false) CategoriaNoticia categoria,
                                        @RequestParam(required = false) EstadoNoticia estado,
                                        @RequestParam(required = false) String titulo) {
        return noticiaService.listarNoticias(categoria, estado, titulo);
    }

    @GetMapping("/{id}")
    public Noticia findById(@PathVariable Long id) {
        return noticiaService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Noticia> crearNoticia(@RequestBody NoticiaDTO datos,
                                                @RequestHeader(RUT) String rut) {
        return ResponseEntity.status(HttpStatus.CREATED).body(noticiaService.crearNoticia(datos, rut));
    }

    @PutMapping("/{id}")
    public Noticia editarNoticia(@PathVariable Long id, @RequestBody NoticiaDTO datos,
                                 @RequestHeader(RUT) String rut) {
        return noticiaService.editarNoticia(id, datos, rut);
    }

    @PatchMapping("/{id}/activar")
    public Noticia activarNoticia(@PathVariable Long id, @RequestHeader(RUT) String rut) {
        return noticiaService.activarNoticia(id, rut);
    }

    @PatchMapping("/{id}/desactivar")
    public Noticia desactivarNoticia(@PathVariable Long id, @RequestHeader(RUT) String rut) {
        return noticiaService.desactivarNoticia(id, rut);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNoticia(@PathVariable Long id, @RequestHeader(RUT) String rut) {
        noticiaService.eliminarNoticia(id, rut);
        return ResponseEntity.noContent().build();
    }
}
