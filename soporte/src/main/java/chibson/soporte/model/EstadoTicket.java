package chibson.soporte.model;

/**
 * ABIERTO → EN_PROCESO → RESUELTO → CERRADO.
 * El administrador puede mover el ticket entre estados (HU-29, HU-31); el cliente lo cierra
 * cuando está RESUELTO (HU-30). CERRADO es el estado final.
 */
public enum EstadoTicket {
    ABIERTO,
    EN_PROCESO,
    RESUELTO,
    CERRADO
}
