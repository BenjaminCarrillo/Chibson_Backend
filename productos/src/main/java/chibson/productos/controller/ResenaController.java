package chibson.productos.controller;

import chibson.productos.client.UsuariosClient;
import chibson.productos.dto.ModeracionDTO;
import chibson.productos.dto.ResenaDTO;
import chibson.productos.dto.ResenasProductoDTO;
import chibson.productos.dto.RespuestaResenaDTO;
import chibson.productos.model.EstadoResena;
import chibson.productos.model.Resena;
import chibson.productos.service.ResenaService;
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
@RequestMapping("/api/resenas")
public class ResenaController {

    private static final String RUT = "X-Rut-Usuario";
    private static final String PERMISO = "GESTIONAR_RESENAS";

    private final ResenaService resenaService;
    private final UsuariosClient usuariosClient;

    public ResenaController(ResenaService resenaService, UsuariosClient usuariosClient) {
        this.resenaService = resenaService;
        this.usuariosClient = usuariosClient;
    }

    /** El cliente con sesión deja su reseña (HU-81). */
    @PostMapping
    public ResponseEntity<Resena> crearResena(@RequestHeader(value = RUT, required = false) String rut,
                                              @RequestBody ResenaDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(resenaService.crearResena(datos, rut));
    }

    /** Público: reseñas aprobadas con promedio y total (HU-82). */
    @GetMapping("/producto/{sku}")
    public ResenasProductoDTO listarAprobadas(@PathVariable String sku) {
        return resenaService.listarAprobadas(sku);
    }

    // ---------- Panel de administración: permiso GESTIONAR_RESENAS ----------

    /** HU-84 y HU-88 (filtrando por sku). */
    @GetMapping("/admin")
    public List<Resena> listarResenas(@RequestHeader(value = RUT, required = false) String rut,
                                      @RequestParam(required = false) EstadoResena estado,
                                      @RequestParam(required = false) String sku,
                                      @RequestParam(required = false) Integer calificacion) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return resenaService.listarResenas(estado, sku, calificacion);
    }

    /** HU-86 */
    @PatchMapping("/{id}/moderar")
    public Resena moderarResena(@RequestHeader(value = RUT, required = false) String rut,
                                @PathVariable Long id, @RequestBody ModeracionDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return resenaService.moderarResena(id, datos.getEstado(), rut);
    }

    /** HU-85 */
    @PatchMapping("/{id}/ocultar")
    public Resena ocultarResena(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long id) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return resenaService.ocultarResena(id, rut);
    }

    /** HU-87 */
    @PutMapping("/{id}/respuesta")
    public Resena responderResena(@RequestHeader(value = RUT, required = false) String rut,
                                  @PathVariable Long id, @RequestBody RespuestaResenaDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return resenaService.responderResena(id, datos.getRespuesta(), rut);
    }

    /** HU-83 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarResena(@RequestHeader(value = RUT, required = false) String rut,
                                               @PathVariable Long id) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        resenaService.eliminarResena(id, rut);
        return ResponseEntity.noContent().build();
    }
}
