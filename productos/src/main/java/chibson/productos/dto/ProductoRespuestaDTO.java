package chibson.productos.dto;

import chibson.productos.model.Categoria;
import chibson.productos.model.EstadoProducto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/** Producto con su stock y su ficha técnica interpretada, listo para mostrar. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductoRespuestaDTO {
    private String sku;
    private String modelo;
    private String marca;
    private String descripcion;
    private Integer precio;
    private Categoria categoria;
    private EstadoProducto estado;
    private String imagenUrl;
    /** Texto original de la ficha técnica. */
    private String fichaTecnica;
    /** Ficha técnica separada en filas; vacía si no hay ficha cargada (HU-19). */
    private List<EspecificacionDTO> especificaciones;
    private Integer stockDisponible;
    private Integer stockMinimo;
    private boolean bajoStock;
    private boolean agotado;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaModificacionFicha;
}
