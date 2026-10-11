package chibson.soporte.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chibson.soporte.dto.CierreDTO;
import chibson.soporte.dto.EstadoDTO;
import chibson.soporte.dto.RespuestaDTO;
import chibson.soporte.dto.TicketDTO;
import chibson.soporte.dto.TicketRespuestaDTO;
import chibson.soporte.model.EstadoTicket;
import chibson.soporte.service.TicketService;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private static final String RUT = "X-Rut-Usuario";

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    // ---------- Cliente ----------

    /** HU-26: responde 201 con el número de ticket. */
    @PostMapping
    public ResponseEntity<TicketRespuestaDTO> crearTicket(@RequestHeader(value = RUT, required = false) String rut,
                                                          @RequestBody TicketDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.crearTicket(rut, datos));
    }

    /** HU-27 */
    @GetMapping("/mis-tickets")
    public List<TicketRespuestaDTO> misTickets(@RequestHeader(value = RUT, required = false) String rut) {
        return ticketService.listarPorCliente(rut);
    }

    /** HU-27 criterio 3: detalle con el historial de respuestas. */
    @GetMapping("/mis-tickets/{numero}")
    public TicketRespuestaDTO miTicket(@RequestHeader(value = RUT, required = false) String rut,
                                       @PathVariable Long numero) {
        return ticketService.obtenerDetalleCliente(numero, rut);
    }

    /** HU-30: cerrar un ticket resuelto y calificar (opcional). */
    @PatchMapping("/mis-tickets/{numero}/cerrar")
    public TicketRespuestaDTO cerrar(@RequestHeader(value = RUT, required = false) String rut,
                                     @PathVariable Long numero, @RequestBody(required = false) CierreDTO datos) {
        return ticketService.cerrarYCalificar(numero, rut, datos);
    }

    // ---------- Administración: permiso GESTIONAR_SOPORTE ----------

    /** HU-28 */
    @GetMapping
    public List<TicketRespuestaDTO> listar(@RequestHeader(value = RUT, required = false) String rut,
                                           @RequestParam(required = false) EstadoTicket estado) {
        return ticketService.listarTickets(rut, estado);
    }

    @GetMapping("/{numero}")
    public TicketRespuestaDTO detalle(@RequestHeader(value = RUT, required = false) String rut,
                                      @PathVariable Long numero) {
        return ticketService.obtenerDetalle(numero, rut);
    }

    /** HU-29 */
    @PostMapping("/{numero}/respuestas")
    public TicketRespuestaDTO responder(@RequestHeader(value = RUT, required = false) String rut,
                                        @PathVariable Long numero, @RequestBody RespuestaDTO datos) {
        return ticketService.responderTicket(numero, datos, rut);
    }

    /** HU-31 */
    @PatchMapping("/{numero}/estado")
    public TicketRespuestaDTO actualizarEstado(@RequestHeader(value = RUT, required = false) String rut,
                                               @PathVariable Long numero, @RequestBody EstadoDTO datos) {
        return ticketService.actualizarEstado(numero, datos.getEstado(), rut);
    }

    /** HU-32. La confirmación ("¿Está seguro?") la pide el frontend antes de llamar. */
    @DeleteMapping("/{numero}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = RUT, required = false) String rut,
                                         @PathVariable Long numero) {
        ticketService.eliminarTicket(numero, rut);
        return ResponseEntity.noContent().build();
    }

    // ---------- Para otros microservicios ----------

    /** ms-informes: tickets sin resolver para el dashboard. */
    @GetMapping("/abiertos/contar")
    public long contarAbiertos() {
        return ticketService.contarAbiertos();
    }
}
