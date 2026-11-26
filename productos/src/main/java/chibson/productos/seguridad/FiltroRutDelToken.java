package chibson.productos.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Hace que el encabezado X-Rut-Usuario que leen los controladores salga SIEMPRE del token ya validado.
 *
 * - Token de un usuario: X-Rut-Usuario = "sub" del token (su RUT).
 * - Token de otro microservicio o petición sin token (rutas públicas): X-Rut-Usuario no existe.
 *
 * Así, aunque alguien llame directo a este microservicio escribiendo otro RUT a mano, se ignora.
 * Los controladores no cambian: siguen usando @RequestHeader("X-Rut-Usuario").
 */
public class FiltroRutDelToken extends OncePerRequestFilter {

    static final String HEADER_RUT = "X-Rut-Usuario";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        String rut = null;
        if (autenticacion instanceof JwtAuthenticationToken token
                && !SeguridadConfig.ROL_SERVICIO.equals(token.getToken().getClaimAsString(SeguridadConfig.CLAIM_ROL))) {
            rut = token.getToken().getSubject();
        }
        chain.doFilter(new PeticionConRut(request, rut), response);
    }

    /** La misma petición, pero con X-Rut-Usuario reemplazado por el RUT del token. */
    private static class PeticionConRut extends HttpServletRequestWrapper {

        private final String rut;

        PeticionConRut(HttpServletRequest peticion, String rut) {
            super(peticion);
            this.rut = rut;
        }

        private boolean esRut(String nombre) {
            return HEADER_RUT.equalsIgnoreCase(nombre);
        }

        @Override
        public String getHeader(String nombre) {
            return esRut(nombre) ? rut : super.getHeader(nombre);
        }

        @Override
        public Enumeration<String> getHeaders(String nombre) {
            if (!esRut(nombre)) {
                return super.getHeaders(nombre);
            }
            return rut == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(rut));
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> nombres = new ArrayList<>();
            for (String nombre : Collections.list(super.getHeaderNames())) {
                if (!esRut(nombre)) {
                    nombres.add(nombre);
                }
            }
            if (rut != null) {
                nombres.add(HEADER_RUT);
            }
            return Collections.enumeration(nombres);
        }
    }
}
