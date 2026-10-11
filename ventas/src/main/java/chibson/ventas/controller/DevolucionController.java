package chibson.ventas.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chibson.ventas.dto.DevolucionDTO;
import chibson.ventas.dto.MotivoDTO;
import chibson.ventas.model.EstadoDevolucion;
import chibson.ventas.model.SolicitudDevolucion;
import chibson.ventas.service.DevolucionService;

@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionController {

    private static final String RUT = "X-Rut-Usuario";

    private final DevolucionService devolucionService;

    public DevolucionController(DevolucionService devolucionService) {
        this.devolucionService = devolucionService;
    }

    // ---------- Cliente ----------

    /** HU-39: responde con el número de seguimiento. */
    @PostMapping
    public ResponseEntity<SolicitudDevolucion> solicitar(@RequestHeader(value = RUT, required = false) String rut,
                                                         @RequestBody DevolucionDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(devolucionService.solicitar(rut, datos));
    }

    /** HU-43 */
    @GetMapping("/mis-devoluciones")
    public List<SolicitudDevolucion> misDevoluciones(@RequestHeader(value = RUT, required = false) String rut) {
        return devolucionService.listarPorCliente(rut);
    }

    // ---------- Administración: permiso GESTIONAR_PEDIDOS ----------

    /** HU-40 */
    @GetMapping
    public List<SolicitudDevolucion> listar(@RequestHeader(value = RUT, required = false) String rut,
                                            @RequestParam(required = false) EstadoDevolucion estado) {
        return devolucionService.listar(rut, estado);
    }

    /** ms-informes (HU-52) */
    @GetMapping("/pendientes/contar")
    public long contarPendientes() {
        return devolucionService.contarPendientes();
    }

    /** HU-40 criterio 2 */
    @GetMapping("/{numero}")
    public SolicitudDevolucion detalle(@RequestHeader(value = RUT, required = false) String rut,
                                       @PathVariable Long numero) {
        return devolucionService.detalle(numero, rut);
    }

    /** HU-41 */
    @PatchMapping("/{numero}/aprobar")
    public SolicitudDevolucion aprobar(@RequestHeader(value = RUT, required = false) String rut,
                                       @PathVariable Long numero) {
        return devolucionService.aprobar(numero, rut);
    }

    /** HU-42 */
    @PatchMapping("/{numero}/rechazar")
    public SolicitudDevolucion rechazar(@RequestHeader(value = RUT, required = false) String rut,
                                        @PathVariable Long numero, @RequestBody MotivoDTO datos) {
        return devolucionService.rechazar(numero, datos.getMotivo(), rut);
    }
}
