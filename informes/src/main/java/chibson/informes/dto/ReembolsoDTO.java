package chibson.informes.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** Producto reembolsado de un pedido, con su fecha (HU-45 criterio 4). Lo entrega ms-ventas. */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoDTO {
    private LocalDateTime fechaReembolso;
    private String sku;
    private Integer cantidad;
    private Integer monto;
}
