package chibson.productos.model;

/** Categorías generales predefinidas (HU-07). Los detalles más finos van en la ficha técnica. */
public enum Categoria {
    GUITARRA("GTR"),
    BAJO("BAJ"),
    PERCUSION("PER"),
    TECLAS("TEC"),
    VIENTO("VIE"),
    CUERDAS("CUE");

    /** Prefijo que se usa al generar el SKU. */
    private final String prefijo;

    Categoria(String prefijo) {
        this.prefijo = prefijo;
    }

    public String getPrefijo() {
        return prefijo;
    }
}
