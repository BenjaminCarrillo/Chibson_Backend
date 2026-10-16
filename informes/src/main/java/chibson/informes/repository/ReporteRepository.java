package chibson.informes.repository;

import chibson.informes.model.OrigenReporte;
import chibson.informes.model.Periodo;
import chibson.informes.model.Reporte;
import chibson.informes.model.TipoReporte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReporteRepository extends JpaRepository<Reporte, Long> {

    List<Reporte> findAllByOrderByFechaCreacionDesc();

    /** Para no repetir un reporte automático del mismo periodo (HU-49 criterio 4). */
    boolean existsByTipoAndPeriodoAndOrigenAndFechaDesde(TipoReporte tipo, Periodo periodo, OrigenReporte origen,
                                                         LocalDate fechaDesde);
}
