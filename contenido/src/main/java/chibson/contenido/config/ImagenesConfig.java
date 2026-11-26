package chibson.contenido.config;

import chibson.contenido.service.ImagenService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Entrega las fotos de la carpeta como archivos normales:
 * GET /imagenes/noticias/<nombre>  →  uploads/<nombre>
 * Por el gateway: http://localhost:8088/imagenes/noticias/<nombre>
 */
@Configuration
public class ImagenesConfig implements WebMvcConfigurer {

    private final ImagenService imagenService;

    public ImagenesConfig(ImagenService imagenService) {
        this.imagenService = imagenService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Queda como "file:///C:/.../uploads/": Spring necesita la barra final
        String ubicacion = imagenService.getCarpeta().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler(ImagenService.RUTA_PUBLICA + "**")
                .addResourceLocations(ubicacion)
                .setCachePeriod(3600);
    }
}
