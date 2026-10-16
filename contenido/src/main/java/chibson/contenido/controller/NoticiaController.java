package chibson.contenido.controller;

import chibson.contenido.client.UsuariosClient;
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
    /** Permiso que exige ms-usuarios para administrar noticias (HU-72). */
    private static final String PERMISO = "GESTIONAR_NOTICIAS";

    private final NoticiaService noticiaService;
    private final UsuariosClient usuariosClient;

    public NoticiaController(NoticiaService noticiaService, UsuariosClient usuariosClient) {
        this.noticiaService = noticiaService;
        this.usuariosClient = usuariosClient;
    }

    // ---------- Vista pública: no requiere sesión ----------

    @GetMapping("/publicadas")
    public List<Noticia> listarPublicadas() {
        return noticiaService.listarPublicadas();
    }

    @GetMapping("/publicadas/{id}")
    public Noticia obtenerPublicada(@PathVariable Long id) {
        return noticiaService.obtenerPublicada(id);
    }

    // ---------- Panel de administración: permiso GESTIONAR_NOTICIAS ----------

    @GetMapping
    public List<Noticia> listarNoticias(@RequestHeader(value = RUT, required = false) String rut,
                                        @RequestParam(required = false) CategoriaNoticia categoria,
                                        @RequestParam(required = false) EstadoNoticia estado,
                                        @RequestParam(required = false) String titulo) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return noticiaService.listarNoticias(categoria, estado, titulo);
    }

    @GetMapping("/{id}")
    public Noticia findById(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long id) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return noticiaService.findById(id);
    }

    @PostMapping
    public ResponseEntity<Noticia> crearNoticia(@RequestHeader(value = RUT, required = false) String rut,
                                                @RequestBody NoticiaDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return ResponseEntity.status(HttpStatus.CREATED).body(noticiaService.crearNoticia(datos, rut));
    }

    @PutMapping("/{id}")
    public Noticia editarNoticia(@RequestHeader(value = RUT, required = false) String rut,
                                 @PathVariable Long id, @RequestBody NoticiaDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return noticiaService.editarNoticia(id, datos, rut);
    }

    @PatchMapping("/{id}/activar")
    public Noticia activarNoticia(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long id) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return noticiaService.activarNoticia(id, rut);
    }

    @PatchMapping("/{id}/desactivar")
    public Noticia desactivarNoticia(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long id) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return noticiaService.desactivarNoticia(id, rut);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarNoticia(@RequestHeader(value = RUT, required = false) String rut,
                                                @PathVariable Long id) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        noticiaService.eliminarNoticia(id, rut);
        return ResponseEntity.noContent().build();
    }
}
