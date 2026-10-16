package chibson.informes.model;

/** Periodo de un reporte (HU-45, HU-48, HU-49). */
public enum Periodo {
    /** Un día. */
    DIARIO,
    /** Una semana de lunes a domingo. */
    SEMANAL,
    /** Un mes completo. */
    MENSUAL,
    /** Un año completo. */
    ANUAL,
    /** Desde y hasta elegidos por el administrador. */
    PERSONALIZADO
}
