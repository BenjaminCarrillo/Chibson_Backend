package chibson.informes.model;

/** Rangos que se pueden elegir en el informe de ventas (HU-45). */
public enum Periodo {
    /** Un día. */
    DIARIO,
    /** Un mes completo. */
    MENSUAL,
    /** Un año completo. */
    ANUAL,
    /** Desde y hasta elegidos por el administrador. */
    PERSONALIZADO
}
