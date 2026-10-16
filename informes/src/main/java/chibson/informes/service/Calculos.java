package chibson.informes.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import chibson.informes.dto.ItemDTO;
import chibson.informes.dto.PedidoDTO;
import chibson.informes.dto.ProductoVendidoDTO;
import chibson.informes.dto.ReembolsoDTO;

/**
 * Cálculos que comparten los reportes. Las ventas se cuentan por la fecha del pedido y los
 * reembolsos por la fecha del reembolso, así un reembolso se descuenta en el periodo en que se
 * registró (HU-45 criterio 4).
 */
public final class Calculos {

    private Calculos() {
    }

    /** Ventas (pagadas y no canceladas) cuya fecha está entre desde y hasta, ambos incluidos. */
    public static List<PedidoDTO> ventasEntre(List<PedidoDTO> pedidos, LocalDate desde, LocalDate hasta) {
        return pedidos.stream()
                .filter(PedidoDTO::esVenta)
                .filter(p -> entre(p.getFecha(), desde, hasta))
                .toList();
    }

    /** Reembolsos registrados entre desde y hasta, de cualquier pedido. */
    public static List<ReembolsoDTO> reembolsosEntre(List<PedidoDTO> pedidos, LocalDate desde, LocalDate hasta) {
        List<ReembolsoDTO> lista = new ArrayList<>();
        for (PedidoDTO p : pedidos) {
            for (ReembolsoDTO r : p.getReembolsos()) {
                if (r.getFechaReembolso() != null && entre(r.getFechaReembolso(), desde, hasta)) {
                    lista.add(r);
                }
            }
        }
        return lista;
    }

    public static boolean entre(LocalDateTime fechaHora, LocalDate desde, LocalDate hasta) {
        LocalDate fecha = fechaHora.toLocalDate();
        return (desde == null || !fecha.isBefore(desde)) && (hasta == null || !fecha.isAfter(hasta));
    }

    public static int sumarTotales(List<PedidoDTO> pedidos) {
        return pedidos.stream().mapToInt(PedidoDTO::getTotal).sum();
    }

    public static int sumarReembolsos(List<ReembolsoDTO> reembolsos) {
        return reembolsos.stream().mapToInt(ReembolsoDTO::getMonto).sum();
    }

    public static long unidadesReembolsadas(List<ReembolsoDTO> reembolsos) {
        return reembolsos.stream().mapToLong(ReembolsoDTO::getCantidad).sum();
    }

    /** Ventas del periodo menos los reembolsos del periodo. */
    public static int totalNeto(List<PedidoDTO> ventas, List<ReembolsoDTO> reembolsos) {
        return sumarTotales(ventas) - sumarReembolsos(reembolsos);
    }

    /** Total dividido por número de ventas, redondeado. 0 si no hay ventas. */
    public static int ticketPromedio(int total, long cantidad) {
        return cantidad == 0 ? 0 : (int) Math.round((double) total / cantidad);
    }

    /** Nombre y categoría de cada SKU, tomados de los productos de los pedidos. */
    public static Map<String, ItemDTO> productosPorSku(List<PedidoDTO> pedidos) {
        Map<String, ItemDTO> porSku = new HashMap<>();
        for (PedidoDTO p : pedidos) {
            for (ItemDTO item : p.getItems()) {
                porSku.putIfAbsent(item.getSku(), item);
            }
        }
        return porSku;
    }

    /**
     * Unidades e ingresos por producto, descontando lo reembolsado (HU-47 criterio 4), de más a
     * menos unidades. categoria = null para no filtrar. Los productos que quedan en 0 no aparecen.
     */
    public static List<ProductoVendidoDTO> productosVendidos(List<PedidoDTO> ventas, List<ReembolsoDTO> reembolsos,
                                                             Map<String, ItemDTO> productos, String categoria) {
        Map<String, ProductoVendidoDTO> porSku = new LinkedHashMap<>();
        for (PedidoDTO venta : ventas) {
            for (ItemDTO item : venta.getItems()) {
                if (categoria != null && !categoria.equalsIgnoreCase(item.getCategoria())) {
                    continue;
                }
                ProductoVendidoDTO p = porSku.computeIfAbsent(item.getSku(),
                        sku -> new ProductoVendidoDTO(sku, item.getNombreProducto(), item.getCategoria(), 0, 0));
                p.setUnidades(p.getUnidades() + item.getCantidad());
                p.setIngresos(p.getIngresos() + item.subtotal());
            }
        }
        for (ReembolsoDTO r : reembolsos) {
            ItemDTO producto = productos.get(r.getSku());
            String nombre = producto == null ? r.getSku() : producto.getNombreProducto();
            String categoriaProducto = producto == null ? null : producto.getCategoria();
            if (categoria != null && !categoria.equalsIgnoreCase(categoriaProducto)) {
                continue;
            }
            ProductoVendidoDTO p = porSku.computeIfAbsent(r.getSku(),
                    sku -> new ProductoVendidoDTO(sku, nombre, categoriaProducto, 0, 0));
            p.setUnidades(p.getUnidades() - r.getCantidad());
            p.setIngresos(p.getIngresos() - r.getMonto());
        }
        List<ProductoVendidoDTO> lista = new ArrayList<>(porSku.values().stream()
                .filter(p -> p.getUnidades() > 0)
                .toList());
        lista.sort(Comparator.comparingInt(ProductoVendidoDTO::getUnidades).reversed()
                .thenComparing(Comparator.comparingInt(ProductoVendidoDTO::getIngresos).reversed()));
        return lista;
    }

    /** Monto vendido por categoría menos lo reembolsado, de mayor a menor. Las que quedan en 0 no aparecen. */
    public static Map<String, Integer> ventasPorCategoria(List<PedidoDTO> ventas, List<ReembolsoDTO> reembolsos,
                                                          Map<String, ItemDTO> productos) {
        Map<String, Integer> porCategoria = new HashMap<>();
        for (PedidoDTO venta : ventas) {
            for (ItemDTO item : venta.getItems()) {
                porCategoria.merge(categoria(item.getCategoria()), item.subtotal(), Integer::sum);
            }
        }
        for (ReembolsoDTO r : reembolsos) {
            ItemDTO producto = productos.get(r.getSku());
            porCategoria.merge(categoria(producto == null ? null : producto.getCategoria()), -r.getMonto(), Integer::sum);
        }
        Map<String, Integer> ordenado = new LinkedHashMap<>();
        porCategoria.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .forEach(e -> ordenado.put(e.getKey(), e.getValue()));
        return ordenado;
    }

    /** Porcentaje de cambio con un decimal, o null si el periodo anterior no tuvo ventas netas. */
    public static Double variacion(int actual, int anterior) {
        return anterior <= 0 ? null : Math.round((actual - anterior) * 1000.0 / anterior) / 10.0;
    }

    private static String categoria(String categoria) {
        return categoria == null ? "SIN_CATEGORIA" : categoria;
    }
}
