package chibson.informes.dto;

import chibson.informes.model.OrigenReporte;
import chibson.informes.model.Periodo;
import chibson.informes.model.Reporte;
import chibson.informes.model.TipoReporte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila del listado de reportes guardados (HU-60), sin el detalle. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReporteListadoDTO {
    private Long idReporte;
    private TipoReporte tipo;
    private Periodo periodo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    private OrigenReporte origen;
    private String rutAdministrador;
    private LocalDateTime fechaCreacion;

    public static ReporteListadoDTO desde(Reporte r) {
        return new ReporteListadoDTO(r.getIdReporte(), r.getTipo(), r.getPeriodo(), r.getFechaDesde(),
                r.getFechaHasta(), r.getOrigen(), r.getRutAdministrador(), r.getFechaCreacion());
    }
}
