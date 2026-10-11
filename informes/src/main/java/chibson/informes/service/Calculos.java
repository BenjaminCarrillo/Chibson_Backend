package chibson.informes.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import chibson.informes.dto.DetalleDTO;
import chibson.informes.dto.PedidoDTO;
import chibson.informes.dto.ProductoVendidoDTO;

/** Cálculos que comparten el dashboard y los informes. */
public final class Calculos {

    private Calculos() {
    }

    /** Ventas (pagadas y no canceladas) cuya fecha está entre desde y hasta, ambos incluidos. */
    public static List<PedidoDTO> ventasEntre(List<PedidoDTO> pedidos, LocalDate desde, LocalDate hasta) {
        return pedidos.stream()
                .filter(PedidoDTO::esVenta)
                .filter(p -> entre(p, desde, hasta))
                .toList();
    }

    public static boolean entre(PedidoDTO p, LocalDate desde, LocalDate hasta) {
        LocalDate fecha = p.getFecha().toLocalDate();
        return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
    }

    public static int sumarTotales(List<PedidoDTO> pedidos) {
        return pedidos.stream().mapToInt(PedidoDTO::getTotal).sum();
    }

    /** Total vendido dividido por número de ventas, redondeado. 0 si no hay ventas. */
    public static int ticketPromedio(int total, long cantidad) {
        return cantidad == 0 ? 0 : (int) Math.round((double) total / cantidad);
    }

    /**
     * Suma unidades e ingresos por producto y los ordena de más a menos unidades.
     * categoria = null para no filtrar.
     */
    public static List<ProductoVendidoDTO> productosVendidos(List<PedidoDTO> ventas, String categoria) {
        Map<String, ProductoVendidoDTO> porSku = new LinkedHashMap<>();
        for (PedidoDTO venta : ventas) {
            for (DetalleDTO d : venta.getDetalles()) {
                if (categoria != null && !categoria.equalsIgnoreCase(d.getCategoria())) {
                    continue;
                }
                ProductoVendidoDTO p = porSku.computeIfAbsent(d.getSku(),
                        sku -> new ProductoVendidoDTO(sku, d.getNombreProducto(), d.getCategoria(), 0, 0));
                p.setUnidades(p.getUnidades() + d.getCantidad());
                p.setIngresos(p.getIngresos() + d.subtotal());
            }
        }
        List<ProductoVendidoDTO> lista = new ArrayList<>(porSku.values());
        lista.sort(Comparator.comparingInt(ProductoVendidoDTO::getUnidades).reversed()
                .thenComparing(Comparator.comparingInt(ProductoVendidoDTO::getIngresos).reversed()));
        return lista;
    }
}
