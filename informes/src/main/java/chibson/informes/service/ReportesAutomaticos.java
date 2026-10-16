package chibson.informes.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import chibson.informes.client.VentasClient;
import chibson.informes.dto.PedidoDTO;

/**
 * Generación automática del resumen general (HU-49). Necesita @EnableScheduling en InformesApplication.
 * Si un microservicio no responde, el error queda en el log y el periodo se genera la próxima vez
 * que el servicio arranque (criterio 5).
 */
@Component
public class ReportesAutomaticos implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ReportesAutomaticos.class);
    private static final String ZONA = "America/Santiago";

    private final ReporteService reporteService;
    private final VentasClient ventasClient;
    private final boolean activos;

    public ReportesAutomaticos(ReporteService reporteService, VentasClient ventasClient,
                               @Value("${reportes.automaticos.activos:true}") boolean activos) {
        this.reporteService = reporteService;
        this.ventasClient = ventasClient;
        this.activos = activos;
    }

    /** Criterio 1: cada lunes a las 00:05, la semana anterior (lunes a domingo). */
    @Scheduled(cron = "0 5 0 * * MON", zone = ZONA)
    public void resumenSemanal() {
        if (activos) {
            generarSemana(LocalDate.now().with(DayOfWeek.MONDAY).minusWeeks(1));
        }
    }

    /** Criterio 2: el día 1 de cada mes a las 00:10, el mes anterior. */
    @Scheduled(cron = "0 10 0 1 * *", zone = ZONA)
    public void resumenMensual() {
        if (activos) {
            generarMes(YearMonth.now().minusMonths(1));
        }
    }

    /**
     * Criterio 5: al iniciar, genera los resúmenes de las semanas y meses ya cerrados que falten,
     * desde el primer pedido registrado en ms-ventas.
     */
    @Override
    public void run(String... args) {
        if (!activos) {
            return;
        }
        LocalDate primerPedido;
        try {
            List<PedidoDTO> pedidos = ventasClient.obtenerPedidos(null, null);
            primerPedido = pedidos.stream()
                    .map(p -> p.getFecha().toLocalDate())
                    .min(LocalDate::compareTo)
                    .orElse(null);
        } catch (RuntimeException e) {
            log.warn("No se generaron los reportes automáticos pendientes: {}", e.getMessage());
            return;
        }
        if (primerPedido == null) {
            log.info("No hay pedidos todavía: no hay reportes automáticos pendientes");
            return;
        }
        LocalDate lunesActual = LocalDate.now().with(DayOfWeek.MONDAY);
        for (LocalDate lunes = primerPedido.with(DayOfWeek.MONDAY); lunes.isBefore(lunesActual); lunes = lunes.plusWeeks(1)) {
            generarSemana(lunes);
        }
        YearMonth mesActual = YearMonth.now();
        for (YearMonth mes = YearMonth.from(primerPedido); mes.isBefore(mesActual); mes = mes.plusMonths(1)) {
            generarMes(mes);
        }
    }

    private void generarSemana(LocalDate lunes) {
        try {
            if (reporteService.generarResumenSemanal(lunes) != null) {
                log.info("Resumen general semanal generado: {} a {}", lunes, lunes.plusDays(6));
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo generar el resumen semanal del {}: {}", lunes, e.getMessage());
        }
    }

    private void generarMes(YearMonth mes) {
        try {
            if (reporteService.generarResumenMensual(mes) != null) {
                log.info("Resumen general mensual generado: {}", mes);
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo generar el resumen mensual de {}: {}", mes, e.getMessage());
        }
    }
}
