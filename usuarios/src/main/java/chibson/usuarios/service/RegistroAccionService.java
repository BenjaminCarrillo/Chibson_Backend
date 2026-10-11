package chibson.usuarios.service;

import chibson.usuarios.dto.DetalleCambioDTO;
import chibson.usuarios.dto.RegistroAccionDTO;
import chibson.usuarios.model.DetalleCambio;
import chibson.usuarios.model.RegistroAccion;
import chibson.usuarios.model.TipoAccion;
import chibson.usuarios.repository.RegistroAccionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Historial de acciones de los administradores (HU-73). */
@Service
public class RegistroAccionService {

    private final RegistroAccionRepository repository;

    public RegistroAccionService(RegistroAccionRepository repository) {
        this.repository = repository;
    }

    /** Guarda una acción enviada por otro microservicio. */
    public RegistroAccion registrarAccion(RegistroAccionDTO datos) {
        Validaciones.obligatorio(datos.getRutAdministrador(), "rutAdministrador");
        Validaciones.obligatorio(datos.getModulo(), "modulo");
        Validaciones.obligatorio(datos.getDescripcion(), "descripcion");
        TipoAccion tipo;
        try {
            tipo = TipoAccion.valueOf(Validaciones.obligatorio(datos.getTipo(), "tipo").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de acción no válido: " + datos.getTipo());
        }
        return registrar(datos.getRutAdministrador(), tipo, datos.getModulo(), datos.getDescripcion(), datos.getCambios());
    }

    /** Guarda una acción hecha dentro de este mismo microservicio. */
    public RegistroAccion registrar(String rutAdministrador, TipoAccion tipo, String modulo,
                                    String descripcion, List<DetalleCambioDTO> cambios) {
        RegistroAccion registro = new RegistroAccion();
        registro.setRutAdministrador(rutAdministrador);
        registro.setTipo(tipo);
        registro.setModulo(modulo);
        registro.setDescripcion(descripcion);
        registro.setFecha(LocalDateTime.now());
        if (cambios != null) {
            for (DetalleCambioDTO c : cambios) {
                registro.getCambios().add(new DetalleCambio(null, c.getCampo(), c.getValorAnterior(), c.getValorNuevo()));
            }
        }
        return repository.save(registro);
    }

    /** Listado de Historial.html, con filtros opcionales y combinables. */
    public List<RegistroAccion> listarAcciones(String texto, TipoAccion tipo, String rutAdministrador,
                                               LocalDate desde, LocalDate hasta) {
        String busqueda = (texto == null) ? "" : texto.trim().toLowerCase();
        return repository.findAllByOrderByFechaDesc().stream()
                .filter(r -> tipo == null || r.getTipo() == tipo)
                .filter(r -> rutAdministrador == null || rutAdministrador.isBlank()
                        || r.getRutAdministrador().equalsIgnoreCase(rutAdministrador.trim()))
                .filter(r -> desde == null || !r.getFecha().toLocalDate().isBefore(desde))
                .filter(r -> hasta == null || !r.getFecha().toLocalDate().isAfter(hasta))
                .filter(r -> busqueda.isEmpty()
                        || r.getDescripcion().toLowerCase().contains(busqueda)
                        || r.getRutAdministrador().toLowerCase().contains(busqueda))
                .toList();
    }

    /** Acciones hechas por un administrador específico (HU-73, criterio 4). */
    public List<RegistroAccion> listarPorAdministrador(String rut) {
        return repository.findByRutAdministradorOrderByFechaDesc(rut);
    }

    public RegistroAccion obtenerDetalle(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la acción " + id));
    }

    /** Agrega un cambio a la lista solo si el valor realmente cambió. */
    public static void anotar(List<DetalleCambioDTO> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new DetalleCambioDTO(campo,
                    anterior == null ? null : anterior.toString(),
                    nuevo == null ? null : nuevo.toString()));
        }
    }

    public static List<DetalleCambioDTO> sinCambios() {
        return new ArrayList<>();
    }
}
