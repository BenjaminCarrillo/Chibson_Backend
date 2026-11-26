package chibson.gateway.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Valida en la entrada los JWT que emite ms-usuarios, con el mismo NimbusJwtDecoder que usan los
 * microservicios (HS256 con jwt.secreto). Si alguien modifica el token, la firma deja de calzar.
 */
@Component
public class JwtServicio {

    private final JwtDecoder decodificador;

    public JwtServicio(@Value("${jwt.secreto}") String secreto) {
        this.decodificador = NimbusJwtDecoder
                .withSecretKey(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /**
     * Devuelve el token leído si la firma es válida y no ha expirado.
     * Si no, lanza JwtException (o JwtValidationException si venció).
     */
    public Jwt validar(String token) {
        return decodificador.decode(token);
    }
}
