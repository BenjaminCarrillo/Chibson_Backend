package chibson.ventas.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Token que usa ms-ventas para identificarse cuando llama a OTRO microservicio.
 * Lleva rol=SERVICIO: con él se puede entrar a los endpoints internos (descontar stock, verificar permisos,
 * contar pedidos...), que un usuario no puede usar aunque tenga sesión.
 * Se firma con la misma clave (jwt.secreto) y se renueva solo antes de vencer.
 */
@Component
public class TokenServicio {

    private static final long MINUTOS_DE_VIDA = 10;

    private final JwtEncoder jwtEncoder;
    private final String nombre;
    private String token;
    private Instant vence = Instant.EPOCH;

    public TokenServicio(JwtEncoder jwtEncoder, @Value("${spring.application.name:ventas}") String nombre) {
        this.jwtEncoder = jwtEncoder;
        this.nombre = "ms-" + nombre;
    }

    public synchronized String obtener() {
        Instant ahora = Instant.now();
        // Se crea uno nuevo si no hay o si le queda menos de 1 minuto
        if (token == null || ahora.isAfter(vence.minusSeconds(60))) {
            vence = ahora.plus(MINUTOS_DE_VIDA, ChronoUnit.MINUTES);
            JwtClaimsSet datos = JwtClaimsSet.builder()
                    .issuer(nombre)
                    .subject(nombre)
                    .issuedAt(ahora)
                    .expiresAt(vence)
                    .claim(SeguridadConfig.CLAIM_ROL, SeguridadConfig.ROL_SERVICIO)
                    .build();
            token = jwtEncoder.encode(JwtEncoderParameters.from(datos)).getTokenValue();
        }
        return token;
    }
}
