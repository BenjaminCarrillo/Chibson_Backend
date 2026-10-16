package chibson.informes.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.informes.client.UsuariosClient;
import chibson.informes.dto.GenerarReporteDTO;
import chibson.informes.dto.ReporteListadoDTO;
import chibson.informes.model.OrigenReporte;
import chibson.informes.model.Periodo;
import chibson.informes.model.Reporte;
import chibson.informes.model.TipoReporte;
import chibson.informes.repository.ReporteRepository;

/**
 * Reportes guardados: generación manual (HU-48), resumen general automático (HU-49, HU-50),
 * listado (HU-60), detalle (HU-61) y eliminación (HU-62).
 */
@Service
public class ReporteService {

    private final ReporteRepository reporteRepository;
    private final GeneradorReportes generador;
    private final UsuariosClient usuariosClient;

    public ReporteService(ReporteRepository reporteRepository, GeneradorReportes generador,
                          UsuariosClient usuariosClient) {
        this.reporteRepository = reporteRepository;
        this.generador = generador;
        this.usuariosClient = usuariosClient;
    }

    // ---------- Manuales ----------

    /** HU-48: el administrador elige tipo y periodo; el reporte queda guardado con origen MANUAL. */
    @Transactional
    public Reporte generarManual(String rutAdmin, GenerarReporteDTO datos) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        if (datos.getTipo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe elegir el tipo: VENTAS_POR_PERIODO, ESTADO_PEDIDOS o PRODUCTOS_MAS_VENDIDOS");
        }
        if (datos.getTipo() == TipoReporte.RESUMEN_GENERAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El resumen general solo se genera automáticamente, cada semana y cada mes");
        }
        if (datos.getPeriodo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Debe elegir un periodo: DIARIO, SEMANAL, MENSUAL, ANUAL o PERSONALIZADO");
        }
        LocalDate[] rango = resolverRango(datos.getPeriodo(), datos.getDesde(), datos.getHasta());

        Reporte reporte = nuevo(datos.getTipo(), datos.getPeriodo(), rango[0], rango[1], OrigenReporte.MANUAL);
        reporte.setRutAdministrador(rutAdmin.trim());
        switch (datos.getTipo()) {
            case VENTAS_POR_PERIODO -> generador.ventasPorPeriodo(reporte);
            case ESTADO_PEDIDOS -> generador.estadoPedidos(reporte);
            case PRODUCTOS_MAS_VENDIDOS -> {
                String categoria = datos.getCategoria();
                reporte.setCategoria(categoria == null || categoria.isBlank() ? null : categoria.trim().toUpperCase());
                generador.productosMasVendidos(reporte);
            }
            default -> throw new IllegalStateException("Tipo no manual: " + datos.getTipo());
        }
        return reporteRepository.save(reporte);
    }

    // ---------- Automáticos ----------

    /** HU-49 criterio 1: resumen general de la semana (lunes a domingo) que empieza en "lunes". */
    @Transactional
    public Reporte generarResumenSemanal(LocalDate lunes) {
        if (existeAutomatico(Periodo.SEMANAL, lunes)) {
            return null;
        }
        Reporte reporte = nuevo(TipoReporte.RESUMEN_GENERAL, Periodo.SEMANAL, lunes, lunes.plusDays(6),
                OrigenReporte.AUTOMATICO);
        generador.resumenGeneral(reporte, lunes.minusWeeks(1), lunes.minusDays(1));
        return reporteRepository.save(reporte);
    }

    /** HU-49 criterio 2: resumen general del mes indicado. */
    @Transactional
    public Reporte generarResumenMensual(YearMonth mes) {
        if (existeAutomatico(Periodo.MENSUAL, mes.atDay(1))) {
            return null;
        }
        Reporte reporte = nuevo(TipoReporte.RESUMEN_GENERAL, Periodo.MENSUAL, mes.atDay(1), mes.atEndOfMonth(),
                OrigenReporte.AUTOMATICO);
        YearMonth anterior = mes.minusMonths(1);
        generador.resumenGeneral(reporte, anterior.atDay(1), anterior.atEndOfMonth());
        return reporteRepository.save(reporte);
    }

    /** HU-49 criterio 4: un periodo que ya tiene su reporte automático no se genera de nuevo. */
    public boolean existeAutomatico(Periodo periodo, LocalDate fechaDesde) {
        return reporteRepository.existsByTipoAndPeriodoAndOrigenAndFechaDesde(TipoReporte.RESUMEN_GENERAL, periodo,
                OrigenReporte.AUTOMATICO, fechaDesde);
    }

    // ---------- Consultas ----------

    /** HU-60: del más reciente al más antiguo, con filtros opcionales por tipo, origen y fecha de creación. */
    @Transactional(readOnly = true)
    public List<ReporteListadoDTO> listar(String rutAdmin, TipoReporte tipo, OrigenReporte origen,
                                          LocalDate desde, LocalDate hasta) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        validarRango(desde, hasta);
        return reporteRepository.findAllByOrderByFechaCreacionDesc().stream()
                .filter(r -> tipo == null || r.getTipo() == tipo)
                .filter(r -> origen == null || r.getOrigen() == origen)
                .filter(r -> Calculos.entre(r.getFechaCreacion(), desde, hasta))
                .map(ReporteListadoDTO::desde)
                .toList();
    }

    /** HU-61: el reporte tal como se generó. */
    @Transactional(readOnly = true)
    public Reporte obtener(String rutAdmin, Long idReporte) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        Reporte reporte = buscar(idReporte);
        reporte.getFilas().size();
        return reporte;
    }

    /** HU-62: la confirmación la pide el frontend antes de llamar a este endpoint. */
    @Transactional
    public void eliminar(String rutAdmin, Long idReporte) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_INFORMES);
        reporteRepository.delete(buscar(idReporte));
    }

    // ---------- Apoyo ----------

    private Reporte buscar(Long idReporte) {
        return reporteRepository.findById(idReporte)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el reporte #" + idReporte));
    }

    private Reporte nuevo(TipoReporte tipo, Periodo periodo, LocalDate desde, LocalDate hasta, OrigenReporte origen) {
        Reporte reporte = new Reporte();
        reporte.setTipo(tipo);
        reporte.setPeriodo(periodo);
        reporte.setFechaDesde(desde);
        reporte.setFechaHasta(hasta);
        reporte.setOrigen(origen);
        reporte.setFechaCreacion(LocalDateTime.now());
        return reporte;
    }

    /**
     * Convierte el periodo elegido en un rango de fechas. DIARIO, SEMANAL, MENSUAL y ANUAL usan el
     * día, la semana (lunes a domingo), el mes o el año de "desde", u hoy si no viene.
     */
    private LocalDate[] resolverRango(Periodo periodo, LocalDate desde, LocalDate hasta) {
        LocalDate referencia = desde == null ? LocalDate.now() : desde;
        return switch (periodo) {
            case DIARIO -> new LocalDate[] {referencia, referencia};
            case SEMANAL -> {
                LocalDate lunes = referencia.with(DayOfWeek.MONDAY);
                yield new LocalDate[] {lunes, lunes.plusDays(6)};
            }
            case MENSUAL -> new LocalDate[] {referencia.withDayOfMonth(1), YearMonth.from(referencia).atEndOfMonth()};
            case ANUAL -> new LocalDate[] {referencia.withDayOfYear(1),
                    referencia.withDayOfYear(referencia.lengthOfYear())};
            case PERSONALIZADO -> {
                if (desde == null || hasta == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "Para un periodo personalizado debe indicar desde y hasta");
                }
                validarRango(desde, hasta);
                yield new LocalDate[] {desde, hasta};
            }
        };
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La fecha desde no puede ser posterior a la fecha hasta");
        }
    }
}
