package chibson.ventas.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import chibson.ventas.model.EstadoDevolucion;
import chibson.ventas.model.ItemDevolucion;
import chibson.ventas.model.Pedido;
import chibson.ventas.model.SolicitudDevolucion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Un producto reembolsado de un pedido: qué se devolvió, cuántas unidades, por cuánto y cuándo
 * (HU-59 criterio 4). ms-informes lo usa para descontar los reembolsos en el periodo de su fecha.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReembolsoDTO {
    private LocalDateTime fechaReembolso;
    private String sku;
    private Integer cantidad;
    private Integer monto;

    /** Reembolsos de las devoluciones REEMBOLSADAS del pedido, una fila por producto devuelto. */
    public static List<ReembolsoDTO> delPedido(Pedido pedido) {
        List<ReembolsoDTO> reembolsos = new ArrayList<>();
        for (SolicitudDevolucion solicitud : pedido.getDevoluciones()) {
            if (solicitud.getEstado() != EstadoDevolucion.REEMBOLSADA) {
                continue;
            }
            for (ItemDevolucion item : solicitud.getItems()) {
                reembolsos.add(new ReembolsoDTO(solicitud.getFechaReembolso(), item.getSku(), item.getCantidad(),
                        item.subtotal()));
            }
        }
        return reembolsos;
    }
}
