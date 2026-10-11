package chibson.contenido.service;

import chibson.contenido.client.UsuariosClient;
import chibson.contenido.dto.DetalleCambioDTO;
import chibson.contenido.dto.RegistroAccionDTO;
import chibson.contenido.model.InformacionInstitucional;
import chibson.contenido.repository.InformacionInstitucionalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class InformacionInstitucionalService {

    private final InformacionInstitucionalRepository repository;
    private final UsuariosClient usuariosClient;

    public InformacionInstitucionalService(InformacionInstitucionalRepository repository,
                                           UsuariosClient usuariosClient) {
        this.repository = repository;
        this.usuariosClient = usuariosClient;
    }

    /** Devuelve la información guardada, o una vacía si todavía no se ha ingresado. */
    public InformacionInstitucional obtenerInformacion() {
        return repository.findById(InformacionInstitucional.ID_UNICO)
                .orElseGet(() -> {
                    InformacionInstitucional vacia = new InformacionInstitucional();
                    vacia.setIdInformacion(InformacionInstitucional.ID_UNICO);
                    return vacia;
                });
    }

    /** Crea o actualiza misión, visión y datos de contacto (HU-78). */
    public InformacionInstitucional actualizarInformacion(InformacionInstitucional datos, String rutAdministrador) {
        if (vacio(datos.getCorreo()) && vacio(datos.getTelefono()) && vacio(datos.getDireccion())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe indicar al menos un dato de contacto: correo, teléfono o dirección");
        }
        InformacionInstitucional actual = obtenerInformacion();
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        anotar(cambios, "mision", actual.getMision(), datos.getMision());
        anotar(cambios, "vision", actual.getVision(), datos.getVision());
        anotar(cambios, "correo", actual.getCorreo(), datos.getCorreo());
        anotar(cambios, "telefono", actual.getTelefono(), datos.getTelefono());
        anotar(cambios, "direccion", actual.getDireccion(), datos.getDireccion());

        datos.setIdInformacion(InformacionInstitucional.ID_UNICO);
        InformacionInstitucional guardada = repository.save(datos);

        usuariosClient.registrarAccion(new RegistroAccionDTO(rutAdministrador, "MODIFICAR",
                "Información institucional", "Actualizó la información institucional", cambios));
        return guardada;
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private void anotar(List<DetalleCambioDTO> cambios, String campo, String anterior, String nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new DetalleCambioDTO(campo, anterior, nuevo));
        }
    }
}
