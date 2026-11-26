package chibson.productos.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/**
 * Fotos de productos guardadas en una carpeta del backend.
 *
 * - La carpeta es imagenes.carpeta (por defecto "uploads", dentro de la carpeta de ms-productos).
 * - Cada foto se guarda con un nombre aleatorio (3f2a9c...png): no depende del SKU.
 * - En la base de datos el producto guarda SOLO ese nombre (columna imagen).
 * - La página la ve en /imagenes/productos/<nombre> a través del gateway (ver config/ImagenesConfig).
 */
@Service
public class ImagenService {

    public static final String RUTA_PUBLICA = "/imagenes/productos/";
    private static final long TAMANO_MAXIMO = 5L * 1024 * 1024; // 5 MB

    private static final Logger log = LoggerFactory.getLogger(ImagenService.class);

    private final Path carpeta;

    public ImagenService(@Value("${imagenes.carpeta:uploads}") String carpeta) {
        this.carpeta = Paths.get(carpeta).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.carpeta); // se crea sola al arrancar si no existe
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo crear la carpeta de imágenes " + this.carpeta, e);
        }
        // Al arrancar se ve en la consola dónde quedó la carpeta (útil si las imágenes no aparecen)
        log.info("Carpeta de imágenes: {}", this.carpeta);
    }

    public Path getCarpeta() {
        return carpeta;
    }

    /** Guarda la foto y devuelve el nombre con que quedó (lo que se guarda en la base de datos). */
    public String guardar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe elegir una imagen");
        }
        if (archivo.getSize() > TAMANO_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen no puede pesar más de 5 MB");
        }
        // El formato se revisa por el contenido y no por el nombre: un PDF renombrado a .png se rechaza
        String extension;
        try (InputStream entrada = archivo.getInputStream()) {
            extension = detectarExtension(entrada.readNBytes(12));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se pudo leer la imagen");
        }
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen debe ser PNG, JPG, WEBP o GIF");
        }
        String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try (InputStream entrada = archivo.getInputStream()) {
            Files.copy(entrada, carpeta.resolve(nombre));
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudo guardar la imagen");
        }
        return nombre;
    }

    /**
     * Revisa que el nombre que llega al crear o editar un producto sea de una foto que está en la carpeta.
     * Solo se aceptan nombres simples (letras, números, punto, guion): así nadie puede pedir "../otro/archivo".
     */
    public String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La imagen es obligatoria");
        }
        String limpio = nombre.trim();
        if (!limpio.matches("[A-Za-z0-9][A-Za-z0-9._-]*") || !Files.isRegularFile(carpeta.resolve(limpio))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La imagen " + limpio + " no existe en la carpeta de imágenes del backend");
        }
        return limpio;
    }

    /** Dirección para mostrarla en la página (no se guarda: se arma con el nombre). */
    public static String urlPublica(String nombre) {
        return nombre == null ? null : RUTA_PUBLICA + nombre;
    }

    /** Reconoce el formato por sus primeros bytes ("firma" de cada formato). */
    private String detectarExtension(byte[] b) {
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return "png";
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return "jpg";
        if (b.length >= 4 && texto(b, 0, 4).equals("GIF8")) return "gif";
        if (b.length >= 12 && texto(b, 0, 4).equals("RIFF") && texto(b, 8, 12).equals("WEBP")) return "webp";
        return null;
    }

    private String texto(byte[] b, int desde, int hasta) {
        return new String(Arrays.copyOfRange(b, desde, hasta), StandardCharsets.US_ASCII);
    }
}
