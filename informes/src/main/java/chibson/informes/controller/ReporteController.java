package chibson.informes.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chibson.informes.dto.GenerarReporteDTO;
import chibson.informes.dto.ReporteListadoDTO;
import chibson.informes.model.OrigenReporte;
import chibson.informes.model.Reporte;
import chibson.informes.model.TipoReporte;
import chibson.informes.service.ReporteService;

/** Reportes. Todos los endpoints piden el permiso VER_INFORMES. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final String RUT = "X-Rut-Usuario";

    private final ReporteService reporteService;

    public ReporteController(ReporteService reporteService) {
        this.reporteService = reporteService;
    }

    /** HU-48: genera y guarda un reporte manual. */
    @PostMapping
    public ResponseEntity<Reporte> generar(@RequestHeader(value = RUT, required = false) String rut,
                                           @RequestBody GenerarReporteDTO datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reporteService.generarManual(rut, datos));
    }

    /** HU-60. Fechas AAAA-MM-DD, sobre la fecha de creación. */
    @GetMapping
    public List<ReporteListadoDTO> listar(
            @RequestHeader(value = RUT, required = false) String rut,
            @RequestParam(required = false) TipoReporte tipo,
            @RequestParam(required = false) OrigenReporte origen,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reporteService.listar(rut, tipo, origen, desde, hasta);
    }

    /** HU-61 */
    @GetMapping("/{id}")
    public Reporte obtener(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long id) {
        return reporteService.obtener(rut, id);
    }

    /** HU-62 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@RequestHeader(value = RUT, required = false) String rut,
                                         @PathVariable Long id) {
        reporteService.eliminar(rut, id);
        return ResponseEntity.noContent().build();
    }
}
