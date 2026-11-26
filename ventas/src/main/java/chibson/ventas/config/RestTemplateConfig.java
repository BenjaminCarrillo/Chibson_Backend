package chibson.ventas.config;

import chibson.ventas.seguridad.TokenServicio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Cliente HTTP para llamar por REST a otros microservicios.
     * Cada llamada lleva el token de servicio (Authorization: Bearer ...), porque ahora todos los
     * microservicios validan el JWT.
     */
    @Bean
    public RestTemplate restTemplate(TokenServicio tokenServicio) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((peticion, cuerpo, ejecucion) -> {
            peticion.getHeaders().setBearerAuth(tokenServicio.obtener());
            return ejecucion.execute(peticion, cuerpo);
        });
        return restTemplate;
    }
}
