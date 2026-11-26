package chibson.usuarios.seguridad;

import chibson.usuarios.model.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Crea el token JWT que recibe el usuario al iniciar sesión (HU-09).
 * Lo firma el JwtEncoder de Spring Security (HS256 con jwt.secreto, ver SeguridadConfig).
 * Todos los microservicios y el gateway lo validan con esa misma clave.
 *
 * Contenido del token (claims):
 *   iss      → "ms-usuarios"
 *   sub      → RUT del usuario
 *   rol      → ADMINISTRADOR o CLIENTE (Spring Security lo usa como ROLE_ADMINISTRADOR / ROLE_CLIENTE)
 *   username → nombre de usuario
 *   iat/exp  → fecha de emisión y de expiración
 */
@Component
public class JwtServicio {

    private final JwtEncoder jwtEncoder;
    private final long minutosDeVida;

    public JwtServicio(JwtEncoder jwtEncoder, @Value("${jwt.minutos:120}") long minutosDeVida) {
        this.jwtEncoder = jwtEncoder;
        this.minutosDeVida = minutosDeVida;
    }

    public String generarToken(Usuario usuario) {
        Instant ahora = Instant.now();
        JwtClaimsSet datos = JwtClaimsSet.builder()
                .issuer("ms-usuarios")
                .subject(usuario.getRut())
                .claim(SeguridadConfig.CLAIM_ROL, usuario.getRol().getNombre())
                .claim("username", usuario.getUsername())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(minutosDeVida, ChronoUnit.MINUTES))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(datos)).getTokenValue();
    }

    public long getMinutosDeVida() {
        return minutosDeVida;
    }
}
