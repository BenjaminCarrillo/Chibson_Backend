package chibson.informes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @EnableScheduling activa los reportes automáticos semanales y mensuales (HU-49). */
@SpringBootApplication
@EnableScheduling
public class InformesApplication {

	public static void main(String[] args) {
		SpringApplication.run(InformesApplication.class, args);
	}

}
