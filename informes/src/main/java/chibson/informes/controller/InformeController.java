package chibson.informes.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import chibson.informes.model.Informe;
import chibson.informes.model.Periodo;
import chibson.informes.service.InformeService;

/** Informes, permiso VER_INFORMES (HU-45 a HU-47). Fechas en formato AAAA-MM-DD. */
@RestController
@RequestMapping("/api/informes")
public class InformeController {

    private static final String RUT = "X-Rut-Usuario";

    private final InformeService informeService;

    public InformeController(InformeService informeService) {
        this.informeService = informeService;
    }

    /** HU-45 */
    @GetMapping("/ventas")
    public Informe ventasPorPeriodo(@RequestHeader(value = RUT, required = false) String rut,
                                    @RequestParam(required = false) Periodo periodo,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                    @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return informeService.ventasPorPeriodo(rut, periodo, desde, hasta);
    }

    /** HU-46 */
    @GetMapping("/pedidos")
    public Informe estadoPedidos(@RequestHeader(value = RUT, required = false) String rut,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return informeService.estadoPedidos(rut, desde, hasta);
    }

    /** HU-47 */
    @GetMapping("/productos")
    public Informe productosMasVendidos(@RequestHeader(value = RUT, required = false) String rut,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                        @RequestParam(required = false) String categoria) {
        return informeService.productosMasVendidos(rut, desde, hasta, categoria);
    }
}
