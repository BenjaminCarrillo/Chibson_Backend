package chibson.soporte.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /** Cliente HTTP para llamar por REST a otros microservicios. */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
