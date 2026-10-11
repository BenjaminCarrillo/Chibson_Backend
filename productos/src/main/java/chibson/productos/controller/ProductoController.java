package chibson.productos.controller;

import chibson.productos.client.UsuariosClient;
import chibson.productos.dto.FichaTecnicaDTO;
import chibson.productos.dto.ProductoDTO;
import chibson.productos.dto.ProductoRespuestaDTO;
import chibson.productos.model.Categoria;
import chibson.productos.model.EstadoProducto;
import chibson.productos.service.ProductoService;
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

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private static final String RUT = "X-Rut-Usuario";
    private static final String PERMISO = "GESTIONAR_PRODUCTOS";

    private final ProductoService productoService;
    private final UsuariosClient usuariosClient;

    public ProductoController(ProductoService productoService, UsuariosClient usuariosClient) {
        this.productoService = productoService;
        this.usuariosClient = usuariosClient;
    }

    // ---------- Catálogo público ----------

    @GetMapping
    public List<ProductoRespuestaDTO> listarActivos() {
        return productoService.listarActivos();
    }

    /** Detalle con stock y ficha técnica interpretada (HU-19). */
    @GetMapping("/{sku}")
    public ProductoRespuestaDTO obtenerActivo(@PathVariable String sku) {
        return productoService.obtenerActivo(sku);
    }

    /** Lista predefinida para el desplegable del formulario (HU-07). */
    @GetMapping("/categorias")
    public List<Categoria> listarCategorias() {
        return Arrays.asList(Categoria.values());
    }

    // ---------- Panel de administración: permiso GESTIONAR_PRODUCTOS ----------

    @GetMapping("/admin")
    public List<ProductoRespuestaDTO> listarProductos(@RequestHeader(value = RUT, required = false) String rut,
                                                      @RequestParam(required = false) EstadoProducto estado,
                                                      @RequestParam(required = false) Categoria categoria,
                                                      @RequestParam(required = false) String texto) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.listarProductos(estado, categoria, texto);
    }

    @GetMapping("/admin/{sku}")
    public ProductoRespuestaDTO findBySku(@RequestHeader(value = RUT, required = false) String rut,
                                          @PathVariable String sku) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.aRespuesta(productoService.findBySku(sku));
    }

    /** HU-01 */
    @PostMapping
    public ResponseEntity<ProductoRespuestaDTO> crearProducto(@RequestHeader(value = RUT, required = false) String rut,
                                                              @RequestBody ProductoDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crearProducto(datos, rut));
    }

    /** HU-02 */
    @PutMapping("/{sku}")
    public ProductoRespuestaDTO editarProducto(@RequestHeader(value = RUT, required = false) String rut,
                                               @PathVariable String sku, @RequestBody ProductoDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.editarProducto(sku, datos, rut);
    }

    /** HU-03 */
    @PatchMapping("/{sku}/desactivar")
    public ProductoRespuestaDTO desactivarProducto(@RequestHeader(value = RUT, required = false) String rut,
                                                   @PathVariable String sku) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.desactivarProducto(sku, rut);
    }

    /** HU-03 */
    @PatchMapping("/{sku}/reactivar")
    public ProductoRespuestaDTO reactivarProducto(@RequestHeader(value = RUT, required = false) String rut,
                                                  @PathVariable String sku) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.reactivarProducto(sku, rut);
    }

    /** HU-04 */
    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> eliminarProducto(@RequestHeader(value = RUT, required = false) String rut,
                                                 @PathVariable String sku) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        productoService.eliminarProducto(sku, rut);
        return ResponseEntity.noContent().build();
    }

    /** HU-05 */
    @PostMapping("/{sku}/ficha-tecnica")
    public ProductoRespuestaDTO agregarFichaTecnica(@RequestHeader(value = RUT, required = false) String rut,
                                                    @PathVariable String sku, @RequestBody FichaTecnicaDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.agregarFichaTecnica(sku, datos.getTexto(), rut);
    }

    /** HU-06 */
    @PutMapping("/{sku}/ficha-tecnica")
    public ProductoRespuestaDTO editarFichaTecnica(@RequestHeader(value = RUT, required = false) String rut,
                                                   @PathVariable String sku, @RequestBody FichaTecnicaDTO datos) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        return productoService.editarFichaTecnica(sku, datos.getTexto(), rut);
    }
}
