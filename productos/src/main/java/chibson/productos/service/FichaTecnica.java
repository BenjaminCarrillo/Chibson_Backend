package chibson.productos.service;

import chibson.productos.dto.EspecificacionDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

/** Validación e interpretación del texto clave:valor;clave:valor de la ficha técnica. */
public final class FichaTecnica {

    private FichaTecnica() {
    }

    /**
     * Revisa el formato antes de guardar (HU-05, HU-06): cada segmento separado por ";"
     * debe tener exactamente un ":" con clave y valor. Se acepta un ";" final.
     * Si falla, el mensaje indica qué segmento está mal.
     */
    public static String validar(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La ficha técnica no puede estar vacía");
        }
        String limpio = texto.trim();
        if (limpio.endsWith(";")) {
            limpio = limpio.substring(0, limpio.length() - 1);
        }
        String[] segmentos = limpio.split(";", -1);
        for (int i = 0; i < segmentos.length; i++) {
            String segmento = segmentos[i].trim();
            String[] partes = segmento.split(":", -1);
            if (segmento.isEmpty() || partes.length != 2 || partes[0].isBlank() || partes[1].isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El segmento " + (i + 1) + " (\"" + segmento + "\") no tiene el formato clave:valor");
            }
        }
        return limpio;
    }

    /**
     * Separa la ficha en filas para mostrarla (HU-19).
     * Los segmentos mal formados se omiten sin interrumpir el resto.
     */
    public static List<EspecificacionDTO> interpretar(String texto) {
        List<EspecificacionDTO> filas = new ArrayList<>();
        if (texto == null || texto.isBlank()) {
            return filas;
        }
        for (String segmento : texto.split(";")) {
            String[] partes = segmento.split(":", -1);
            if (partes.length == 2 && !partes[0].isBlank() && !partes[1].isBlank()) {
                filas.add(new EspecificacionDTO(capitalizar(partes[0].trim()), partes[1].trim()));
            }
        }
        return filas;
    }

    private static String capitalizar(String clave) {
        return Character.toUpperCase(clave.charAt(0)) + clave.substring(1);
    }
}
