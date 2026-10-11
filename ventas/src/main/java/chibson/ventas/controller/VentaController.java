package chibson.ventas.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chibson.ventas.dto.VentaDTO;
import chibson.ventas.model.MetodoPago;
import chibson.ventas.service.VentaService;

/** Ventas.html, permiso VER_VENTAS (HU-57 a HU-59). */
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private static final String RUT = "X-Rut-Usuario";

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    /** HU-57 y HU-58. Fechas en formato AAAA-MM-DD. */
    @GetMapping
    public List<VentaDTO> listar(@RequestHeader(value = RUT, required = false) String rut,
                                 @RequestParam(required = false) String texto,
                                 @RequestParam(required = false) MetodoPago metodo,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ventaService.listar(rut, texto, metodo, desde, hasta);
    }

    /** HU-59 */
    @GetMapping("/{numero}")
    public VentaDTO detalle(@RequestHeader(value = RUT, required = false) String rut, @PathVariable Long numero) {
        return ventaService.detalle(numero, rut);
    }
}
