package chibson.gateway.seguridad;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Filtro que pasa por TODAS las peticiones antes de llegar a los microservicios.
 *
 * 1. Borra cualquier X-Rut-Usuario que venga del navegador: así nadie puede hacerse pasar por otro
 *    usuario escribiendo un RUT a mano.
 * 2. Si la petición trae "Authorization: Bearer <token>", valida el token (firma y expiración) y
 *    pone el RUT del token (claim "sub") en X-Rut-Usuario. Los microservicios siguen leyendo ese
 *    encabezado como antes, por eso no hubo que cambiarlos.
 * 3. Sin token, o con un token inválido, solo se dejan pasar las rutas públicas (login, registro,
 *    catálogo, noticias...). El resto responde 401.
 */
@Component
public class FiltroJwt implements GlobalFilter, Ordered {

    private static final String HEADER_RUT = "X-Rut-Usuario";
    private static final String PREFIJO = "Bearer ";

    /** Se pueden usar sin iniciar sesión, con cualquier método. */
    private static final List<String> PUBLICAS = List.of(
            "/api/auth/login",
            "/api/auth/registro",
            "/api/auth/recuperar");

    /** Se pueden consultar (GET) sin iniciar sesión: lo que ve un visitante en la tienda. */
    private static final List<String> PUBLICAS_GET = List.of(
            "/api/productos",
            "/api/resenas",
            "/api/noticias",
            "/api/institucional",
            "/imagenes"); // fotos de productos y noticias

    private final JwtServicio jwtServicio;

    public FiltroJwt(JwtServicio jwtServicio) {
        this.jwtServicio = jwtServicio;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest peticion = exchange.getRequest();

        // Las consultas previas de CORS (OPTIONS) no llevan token
        if (HttpMethod.OPTIONS.equals(peticion.getMethod())) {
            return chain.filter(exchange);
        }

        ServerHttpRequest.Builder nueva = peticion.mutate().headers(h -> h.remove(HEADER_RUT));
        boolean publica = esPublica(peticion);
        String autorizacion = peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (autorizacion == null || !autorizacion.startsWith(PREFIJO)) {
            if (publica) {
                return chain.filter(exchange.mutate().request(nueva.build()).build());
            }
            return rechazar(exchange, "Debe iniciar sesión para usar esta función");
        }

        try {
            Claims datos = jwtServicio.validar(autorizacion.substring(PREFIJO.length()).trim());
            nueva.header(HEADER_RUT, datos.getSubject());
        } catch (ExpiredJwtException e) {
            if (!publica) {
                return rechazar(exchange, "La sesión expiró. Inicie sesión nuevamente");
            }
        } catch (JwtException | IllegalArgumentException e) {
            if (!publica) {
                return rechazar(exchange, "El token no es válido. Inicie sesión nuevamente");
            }
        }
        // En una ruta pública con token vencido se sigue como visitante (sin X-Rut-Usuario)
        return chain.filter(exchange.mutate().request(nueva.build()).build());
    }

    private boolean esPublica(ServerHttpRequest peticion) {
        String ruta = peticion.getPath().value();
        if (PUBLICAS.stream().anyMatch(ruta::equals)) {
            return true;
        }
        return HttpMethod.GET.equals(peticion.getMethod())
                && PUBLICAS_GET.stream().anyMatch(p -> ruta.equals(p) || ruta.startsWith(p + "/"));
    }

    /** Responde 401 con el mismo formato de error que usan los microservicios. */
    private Mono<Void> rechazar(ServerWebExchange exchange, String mensaje) {
        ServerHttpResponse respuesta = exchange.getResponse();
        respuesta.setStatusCode(HttpStatus.UNAUTHORIZED);
        respuesta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String json = "{\"status\":401,\"error\":\"Unauthorized\",\"message\":\"" + mensaje
                + "\",\"path\":\"" + exchange.getRequest().getPath().value().replace("\"", "") + "\"}";
        DataBuffer cuerpo = respuesta.bufferFactory().wrap(json.getBytes(StandardCharsets.UTF_8));
        return respuesta.writeWith(Mono.just(cuerpo));
    }

    /** Se ejecuta antes que los filtros que reenvían la petición al microservicio. */
    @Override
    public int getOrder() {
        return -1;
    }
}
