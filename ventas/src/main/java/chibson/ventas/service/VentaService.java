package chibson.ventas.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.client.UsuariosClient;
import chibson.ventas.dto.ReembolsoDTO;
import chibson.ventas.dto.UsuarioDTO;
import chibson.ventas.dto.VentaDTO;
import chibson.ventas.model.MetodoPago;
import chibson.ventas.model.Pedido;
import chibson.ventas.repository.PedidoRepository;

/**
 * Registro de ventas (HU-57 a HU-59). Una venta es un pedido con pago aprobado;
 * el número de venta es el número de pedido. Cada venta muestra lo reembolsado y su total neto.
 */
@Service
public class VentaService {

    private final PedidoRepository pedidoRepository;
    private final UsuariosClient usuariosClient;

    public VentaService(PedidoRepository pedidoRepository, UsuariosClient usuariosClient) {
        this.pedidoRepository = pedidoRepository;
        this.usuariosClient = usuariosClient;
    }

    /** HU-57 y HU-58: ventas con filtros opcionales por texto (número o cliente), método y rango de fechas. */
    @Transactional(readOnly = true)
    public List<VentaDTO> listar(String rutAdmin, String texto, MetodoPago metodo, LocalDate desde, LocalDate hasta) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_VENTAS);
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }
        Map<String, UsuarioDTO> cache = new HashMap<>();
        return pedidoRepository.findAllByOrderByFechaDesc().stream()
                .filter(Pedido::tienePagoAprobado)
                .filter(p -> metodo == null || p.getMetodoPago() == metodo)
                .filter(p -> desde == null || !p.getFecha().toLocalDate().isBefore(desde))
                .filter(p -> hasta == null || !p.getFecha().toLocalDate().isAfter(hasta))
                .map(p -> aDTO(p, cache.computeIfAbsent(p.getRutCliente(), usuariosClient::buscarUsuario)))
                .filter(v -> coincideTexto(v, texto))
                .toList();
    }

    /** HU-59: detalle de una venta. */
    @Transactional(readOnly = true)
    public VentaDTO detalle(Long numeroVenta, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.VER_VENTAS);
        Pedido pedido = pedidoRepository.findById(numeroVenta)
                .filter(Pedido::tienePagoAprobado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la venta #" + numeroVenta));
        return aDTO(pedido, usuariosClient.buscarUsuario(pedido.getRutCliente()));
    }

    private VentaDTO aDTO(Pedido p, UsuarioDTO cliente) {
        return new VentaDTO(p.getNumeroPedido(), p.getRutCliente(),
                cliente == null ? null : cliente.nombreCompleto(), p.getFecha(), p.getTotal(),
                p.montoReembolsado(), p.totalNeto(), p.getMetodoPago(), p.getEstado(),
                new ArrayList<>(p.getItems()), ReembolsoDTO.delPedido(p));
    }

    private boolean coincideTexto(VentaDTO v, String texto) {
        if (texto == null || texto.isBlank()) {
            return true;
        }
        String t = texto.trim().toLowerCase();
        return String.valueOf(v.getNumeroVenta()).equals(t.replace("#", ""))
                || (v.getNombreCliente() != null && v.getNombreCliente().toLowerCase().contains(t))
                || v.getRutCliente().toLowerCase().contains(t);
    }
}
