package chibson.productos.dto;

import chibson.productos.model.Categoria;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos que envía el administrador al crear (HU-01) o editar (HU-02) un producto. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private String modelo;
    private String marca;
    private String descripcion;
    private Integer precio;
    private Categoria categoria;
    private String imagenUrl;
    /** Al crear es el stock inicial; al editar, el nuevo stock disponible. */
    private Integer stockDisponible;
    /** Opcional al crear: si no viene, se usa 5. */
    private Integer stockMinimo;
    /** Solo al crear: false lo deja inactivo. Si no viene, queda activo. */
    private Boolean activo;
}
