package chibson.productos.controller;

import chibson.productos.client.UsuariosClient;
import chibson.productos.service.ImagenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Subida de la foto desde el modal "Agregar / Modificar producto".
 *
 * POST /api/productos/imagen   (multipart/form-data, campo "archivo")
 * Respuesta 201: { "imagen": "3f2a9c...png", "imagenUrl": "/imagenes/productos/3f2a9c...png" }
 *
 * Después el modal envía "imagen" al crear o modificar el producto.
 */
@RestController
@RequestMapping("/api/productos")
public class ImagenController {

    private static final String RUT = "X-Rut-Usuario";
    private static final String PERMISO = "GESTIONAR_PRODUCTOS";

    private final ImagenService imagenService;
    private final UsuariosClient usuariosClient;

    public ImagenController(ImagenService imagenService, UsuariosClient usuariosClient) {
        this.imagenService = imagenService;
        this.usuariosClient = usuariosClient;
    }

    @PostMapping(value = "/imagen", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> subirImagen(@RequestHeader(value = RUT, required = false) String rut,
                                                           @RequestParam("archivo") MultipartFile archivo) {
        usuariosClient.exigirPermiso(rut, PERMISO);
        String nombre = imagenService.guardar(archivo);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("imagen", nombre, "imagenUrl", ImagenService.urlPublica(nombre)));
    }
}
