package chibson.contenido.controller;

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

    public InformacionInstitucionalController(InformacionInstitucionalService service) {
        this.service = service;
    }

    /** Público: lo usa la página "Nosotros". */
    @GetMapping
    public InformacionInstitucional obtenerInformacion() {
        return service.obtenerInformacion();
    }

    /** Administración: crea o actualiza la información. */
    @PutMapping
    public InformacionInstitucional actualizarInformacion(@RequestBody InformacionInstitucional datos,
                                                          @RequestHeader("X-Rut-Usuario") String rut) {
        return service.actualizarInformacion(datos, rut);
    }
}
