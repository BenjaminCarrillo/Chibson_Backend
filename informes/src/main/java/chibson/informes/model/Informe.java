package chibson.informes.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.Data;
import lombok.NoArgsConstructor;

/** Informe generado (HU-45 a HU-47). No se guarda: se arma cada vez que se pide. */
@Data
@NoArgsConstructor
public class Informe {
    private TipoInforme tipo;
    private LocalDate fechaDesde;
    private LocalDate fechaHasta;
    /** Solo en productos más vendidos, si se filtró. */
    private String categoria;
    private Integer totalVentas;
    private Long numeroPedidos;
    private Integer ticketPromedio;
    /** Aviso cuando el informe no tiene datos, para no mostrar una pantalla vacía. */
    private String mensaje;
    private List<Map<String, Object>> filas = new ArrayList<>();
}
