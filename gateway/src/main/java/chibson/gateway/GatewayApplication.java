package chibson.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Puerta de entrada única del backend. El frontend solo conoce http://localhost:8080 y el
 * gateway reenvía cada petición al microservicio que corresponde según la ruta.
 * Las rutas y CORS están en application.yml; no hace falta código.
 */
@SpringBootApplication
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
