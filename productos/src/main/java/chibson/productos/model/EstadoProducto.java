package chibson.productos.model;

/**
 * Un producto INACTIVO no aparece en el catálogo público (HU-03).
 * "Agotado" no es un estado: es tener stock 0 en el inventario.
 */
public enum EstadoProducto {
    ACTIVO,
    INACTIVO
}
