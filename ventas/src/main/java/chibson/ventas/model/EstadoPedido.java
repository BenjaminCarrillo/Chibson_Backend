package chibson.ventas.model;

/**
 * Flujo del pedido: PENDIENTE (esperando pago) → PAGADO → EN_PROCESO → ENVIADO → ENTREGADO.
 * Se puede CANCELAR en cualquier momento antes de ENTREGADO (HU-37).
 */
public enum EstadoPedido {
    PENDIENTE,
    PAGADO,
    EN_PROCESO,
    ENVIADO,
    ENTREGADO,
    CANCELADO
}
