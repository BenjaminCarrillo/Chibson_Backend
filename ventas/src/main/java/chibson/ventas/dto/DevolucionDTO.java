package chibson.ventas.dto;

import chibson.ventas.model.MotivoDevolucion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Solicitud de devolución que envía el cliente (HU-39). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DevolucionDTO {
    private Long numeroPedido;
    private String sku;
    private MotivoDevolucion motivo;
    private String descripcion;
    /** Opcional. */
    private String evidenciaUrl;
}
