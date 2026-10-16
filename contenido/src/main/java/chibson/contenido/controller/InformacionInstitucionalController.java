package chibson.contenido.controller;

import chibson.contenido.client.UsuariosClient;
import chibson.contenido.model.InformacionInstitucional;
import chibson.contenido.service.InformacionInstitucionalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institucional")
public class InformacionInstitucionalController {

    private final InformacionInstitucionalService service;
    private final UsuariosClient usuariosClient;

    public InformacionInstitucionalController(InformacionInstitucionalService service, UsuariosClient usuariosClient) {
        this.service = service;
        this.usuariosClient = usuariosClient;
    }

    /** Público: lo usa la página "Nosotros". */
    @GetMapping
    public InformacionInstitucional obtenerInformacion() {
        return service.obtenerInformacion();
    }

    /** Administración: crea o actualiza la información. Requiere GESTIONAR_NOTICIAS (HU-72). */
    @PutMapping
    public InformacionInstitucional actualizarInformacion(
            @RequestHeader(value = "X-Rut-Usuario", required = false) String rut,
            @RequestBody InformacionInstitucional datos) {
        usuariosClient.exigirPermiso(rut, "GESTIONAR_NOTICIAS");
        return service.actualizarInformacion(datos, rut);
    }
}
