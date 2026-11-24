package chibson.usuarios.seguridad;

import chibson.usuarios.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * Crea el token JWT que recibe el usuario al iniciar sesión (HU-09).
 * El token va firmado con HS256 usando una clave secreta que comparten ms-usuarios y el gateway:
 * el gateway la usa para comprobar que el token no fue modificado y que no ha expirado.
 *
 * Contenido del token (claims):
 *   sub      → RUT del usuario (lo usa el gateway para armar el encabezado X-Rut-Usuario)
 *   rol      → CLIENTE, ADMINISTRADOR, ...
 *   username → nombre de usuario
 *   iat/exp  → fecha de emisión y de expiración
 */
@Component
public class JwtServicio {

    private final SecretKey clave;
    private final long minutosDeVida;

    public JwtServicio(@Value("${jwt.secreto}") String secreto,
                       @Value("${jwt.minutos:120}") long minutosDeVida) {
        // HS256 exige una clave de al menos 32 caracteres (256 bits); si es más corta, jjwt avisa al arrancar
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
        this.minutosDeVida = minutosDeVida;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        return Jwts.builder()
                .subject(usuario.getRut())
                .claim("rol", usuario.getRol().getNombre())
                .claim("username", usuario.getUsername())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(ahora.plus(minutosDeVida, ChronoUnit.MINUTES)))
                .signWith(clave)
                .compact();
    }

    public long getMinutosDeVida() {
        return minutosDeVida;
    }
}
