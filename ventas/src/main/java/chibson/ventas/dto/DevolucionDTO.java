package chibson.ventas.dto;

import java.util.List;

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
    /** Uno o varios productos del pedido, cada uno con las unidades que devuelve: {sku, cantidad}. */
    private List<ItemDTO> items;
    private MotivoDevolucion motivo;
    private String descripcion;
    /** Opcional. */
    private String evidenciaUrl;
}
