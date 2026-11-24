package chibson.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Valida los JWT que emite ms-usuarios al iniciar sesión.
 * Usa la MISMA clave secreta que ms-usuarios (jwt.secreto en application.yml):
 * si alguien modifica el token, la firma deja de calzar y el token se rechaza.
 */
@Component
public class JwtServicio {

    private final JwtParser parser;

    public JwtServicio(@Value("${jwt.secreto}") String secreto) {
        this.parser = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8)))
                .build();
    }

    /**
     * Devuelve los datos del token si la firma es válida y no ha expirado.
     * Si no, lanza una excepción de jjwt (ExpiredJwtException, SignatureException, MalformedJwtException...).
     */
    public Claims validar(String token) {
        return parser.parseSignedClaims(token).getPayload();
    }
}
