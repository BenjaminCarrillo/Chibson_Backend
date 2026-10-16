package chibson.ventas.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Datos del producto que entrega ms-productos (solo los que ventas necesita). */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ProductoDTO {
    private String sku;
    private String modelo;
    private Integer precio;
    private String categoria;
    private Integer stockDisponible;
}
