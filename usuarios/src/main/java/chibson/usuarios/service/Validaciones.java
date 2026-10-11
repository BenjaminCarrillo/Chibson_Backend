package chibson.usuarios.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Validaciones de RUT, correo y contraseña que usan varios servicios. */
public final class Validaciones {

    private Validaciones() {
    }

    /**
     * Deja el RUT en formato 12345678-5 (sin puntos, con guion y K mayúscula)
     * y comprueba su dígito verificador (HU-08).
     */
    public static String normalizarRut(String rut) {
        if (rut == null || rut.isBlank()) {
            throw error("El RUT es obligatorio");
        }
        String limpio = rut.replace(".", "").replace("-", "").replace(" ", "").toUpperCase();
        if (limpio.length() < 8 || limpio.length() > 9) {
            throw error("El RUT no tiene un formato válido");
        }
        String cuerpo = limpio.substring(0, limpio.length() - 1);
        char dv = limpio.charAt(limpio.length() - 1);
        if (!cuerpo.chars().allMatch(Character::isDigit)) {
            throw error("El RUT no tiene un formato válido");
        }
        if (calcularDv(cuerpo) != dv) {
            throw error("El dígito verificador del RUT no es válido");
        }
        return cuerpo + "-" + dv;
    }

    /** Algoritmo del módulo 11. */
    private static char calcularDv(String cuerpo) {
        int suma = 0;
        int factor = 2;
        for (int i = cuerpo.length() - 1; i >= 0; i--) {
            suma += (cuerpo.charAt(i) - '0') * factor;
            factor = (factor == 7) ? 2 : factor + 1;
        }
        int resto = 11 - (suma % 11);
        if (resto == 11) {
            return '0';
        }
        if (resto == 10) {
            return 'K';
        }
        return (char) ('0' + resto);
    }

    public static String validarCorreo(String correo) {
        if (correo == null || !correo.trim().matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw error("El correo electrónico no tiene un formato válido");
        }
        return correo.trim().toLowerCase();
    }

    /** Mínimo 8 caracteres, con al menos una letra y un número (HU-08, HU-10, HU-16). */
    public static void validarPassword(String password, String confirmacion) {
        if (password == null || password.length() < 8
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw error("La contraseña debe tener al menos 8 caracteres, con letras y números");
        }
        if (confirmacion != null && !password.equals(confirmacion)) {
            throw error("Las contraseñas no coinciden");
        }
    }

    public static String obligatorio(String valor, String nombreCampo) {
        if (valor == null || valor.isBlank()) {
            throw error("El campo " + nombreCampo + " es obligatorio");
        }
        return valor.trim();
    }

    public static String opcional(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private static ResponseStatusException error(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
