package chibson.informes.seguridad;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Seguridad de ms-informes con Spring Security: valida el JWT en CADA petición.
 *
 * - El token se valida con la clave jwt.secreto (HS256), la misma con que ms-usuarios lo firma.
 *   Si la firma no calza o el token venció, Spring responde 401 antes de llegar al controlador.
 * - El claim "rol" del token se convierte en la autoridad ROLE_<rol> (ROLE_ADMINISTRADOR, ROLE_CLIENTE,
 *   ROLE_SERVICIO) para usar hasRole(...).
 * - Los permisos finos (GESTIONAR_PRODUCTOS, etc.) se siguen revisando como antes con ms-usuarios.
 */
@Configuration
public class SeguridadConfig {

    public static final String CLAIM_ROL = "rol";
    /** Rol de los tokens con que los microservicios se llaman entre ellos (ver TokenServicio). */
    public static final String ROL_SERVICIO = "SERVICIO";
    public static final String ROL_ADMINISTRADOR = "ADMINISTRADOR";

    @Bean
    SecurityFilterChain filtroSeguridad(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Página de error de Spring y consultas previas de CORS
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // ms-informes no tiene rutas públicas: todo requiere sesión
                        // Todo lo demás necesita un token válido
                        .anyRequest().authenticated())
                .oauth2ResourceServer(servidor -> servidor
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(conversorDeRoles())))
                // Después de validar el token, X-Rut-Usuario pasa a ser el RUT del token
                .addFilterAfter(new FiltroRutDelToken(), BearerTokenAuthenticationFilter.class)
                .build();
    }

    /** Revisa que la firma HS256 corresponda a jwt.secreto y que el token no haya vencido. */
    @Bean
    JwtDecoder jwtDecoder(@Value("${jwt.secreto}") String secreto) {
        return NimbusJwtDecoder.withSecretKey(clave(secreto))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /** Firma tokens con jwt.secreto. Lo usa TokenServicio para llamar a otros microservicios. */
    @Bean
    JwtEncoder jwtEncoder(@Value("${jwt.secreto}") String secreto) {
        return NimbusJwtEncoder.withSecretKey(clave(secreto))
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    private static SecretKey clave(String secreto) {
        return new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Convierte el claim rol=ADMINISTRADOR en la autoridad ROLE_ADMINISTRADOR. */
    private Converter<Jwt, ? extends AbstractAuthenticationToken> conversorDeRoles() {
        return jwt -> {
            String rol = jwt.getClaimAsString(CLAIM_ROL);
            List<SimpleGrantedAuthority> autoridades = rol == null
                    ? List.of()
                    : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
            return new JwtAuthenticationToken(jwt, autoridades, jwt.getSubject());
        };
    }
}
