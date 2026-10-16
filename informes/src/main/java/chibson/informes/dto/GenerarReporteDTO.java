package chibson.informes.dto;

import chibson.informes.model.Periodo;
import chibson.informes.model.TipoReporte;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Lo que elige el administrador al generar un reporte manual (HU-48).
 * DIARIO, SEMANAL, MENSUAL y ANUAL toman el día, la semana, el mes o el año de "desde" (hoy si no
 * se indica). PERSONALIZADO necesita desde y hasta.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GenerarReporteDTO {
    private TipoReporte tipo;
    private Periodo periodo;
    private LocalDate desde;
    private LocalDate hasta;
    /** Opcional, solo para productos más vendidos (HU-47). */
    private String categoria;
}
