package chibson.ventas.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.client.ProductosClient;
import chibson.ventas.client.UsuariosClient;
import chibson.ventas.dto.DetalleCambioDTO;
import chibson.ventas.dto.DevolucionDTO;
import chibson.ventas.dto.RegistroAccionDTO;
import chibson.ventas.dto.UsuarioDTO;
import chibson.ventas.model.DetallePedido;
import chibson.ventas.model.EstadoDevolucion;
import chibson.ventas.model.EstadoPedido;
import chibson.ventas.model.HistorialEstadoPedido;
import chibson.ventas.model.Pedido;
import chibson.ventas.model.SolicitudDevolucion;
import chibson.ventas.repository.DevolucionRepository;

/** Devoluciones (HU-39 a HU-43). */
@Service
public class DevolucionService {

    /** Plazo para pedir una devolución desde que el pedido se entregó. */
    private static final int DIAS_PLAZO = 10;
    private static final String MODULO = "Devoluciones";

    private final DevolucionRepository devolucionRepository;
    private final PedidoService pedidoService;
    private final ProductosClient productosClient;
    private final UsuariosClient usuariosClient;

    public DevolucionService(DevolucionRepository devolucionRepository, PedidoService pedidoService,
                             ProductosClient productosClient, UsuariosClient usuariosClient) {
        this.devolucionRepository = devolucionRepository;
        this.pedidoService = pedidoService;
        this.productosClient = productosClient;
        this.usuariosClient = usuariosClient;
    }

    /** HU-39: el cliente solicita devolver un producto de un pedido entregado. */
    @Transactional
    public SolicitudDevolucion solicitar(String rutHeader, DevolucionDTO datos) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        if (datos.getNumeroPedido() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de pedido es obligatorio");
        }
        if (datos.getSku() == null || datos.getSku().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el producto a devolver");
        }
        if (datos.getMotivo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un motivo");
        }
        Pedido pedido = pedidoService.buscarDelCliente(datos.getNumeroPedido(), cliente.getRut());
        String sku = datos.getSku().trim().toUpperCase();

        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se pueden devolver productos de pedidos entregados");
        }
        buscarDetalle(pedido, sku);

        LocalDateTime entrega = fechaEntrega(pedido);
        if (entrega.plusDays(DIAS_PLAZO).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plazo de " + DIAS_PLAZO + " días para solicitar la devolución ya venció");
        }
        if (devolucionRepository.existsByPedidoNumeroPedidoAndSkuAndEstadoNot(
                pedido.getNumeroPedido(), sku, EstadoDevolucion.RECHAZADA)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe una solicitud de devolución para este producto en este pedido");
        }

        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        solicitud.setRutCliente(cliente.getRut());
        solicitud.setSku(sku);
        solicitud.setMotivo(datos.getMotivo());
        solicitud.setDescripcion(vacioANull(datos.getDescripcion()));
        solicitud.setEvidenciaUrl(vacioANull(datos.getEvidenciaUrl()));
        solicitud.setEstado(EstadoDevolucion.PENDIENTE);
        solicitud.setFechaSolicitud(LocalDateTime.now());
        solicitud.setPedido(pedido);
        return devolucionRepository.save(solicitud);
    }

    /** HU-43: solicitudes del cliente con su estado. */
    @Transactional(readOnly = true)
    public List<SolicitudDevolucion> listarPorCliente(String rutHeader) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        return devolucionRepository.findByRutClienteOrderByFechaSolicitudDesc(cliente.getRut());
    }

    /** HU-40: todas las solicitudes, opcionalmente filtradas por estado. */
    @Transactional(readOnly = true)
    public List<SolicitudDevolucion> listar(String rutAdmin, EstadoDevolucion estado) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        return devolucionRepository.findAllByOrderByFechaSolicitudDesc().stream()
                .filter(s -> estado == null || s.getEstado() == estado)
                .toList();
    }

    @Transactional(readOnly = true)
    public SolicitudDevolucion detalle(Long numeroSeguimiento, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        return findById(numeroSeguimiento);
    }

    /** HU-41: aprueba la devolución y el producto vuelve al inventario (HU-64). */
    @Transactional
    public SolicitudDevolucion aprobar(Long numeroSeguimiento, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        SolicitudDevolucion solicitud = buscarPendiente(numeroSeguimiento);
        DetallePedido detalle = buscarDetalle(solicitud.getPedido(), solicitud.getSku());

        productosClient.reponerStock(solicitud.getSku(), detalle.getCantidad(), solicitud.getNumeroPedido(), "DEVOLUCION");
        resolver(solicitud, EstadoDevolucion.APROBADA, rutAdmin);
        usuariosClient.enviarNotificacion(solicitud.getRutCliente(),
                "Su devolución #" + numeroSeguimiento + " fue aprobada. "
                        + "Le contactaremos para coordinar el reembolso", "devolucion:" + numeroSeguimiento);
        registrar(rutAdmin, "Aprobó la devolución #" + numeroSeguimiento + " del pedido #" + solicitud.getNumeroPedido(),
                EstadoDevolucion.APROBADA);
        return devolucionRepository.save(solicitud);
    }

    /** HU-42: rechaza la devolución indicando el motivo. */
    @Transactional
    public SolicitudDevolucion rechazar(Long numeroSeguimiento, String motivo, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el motivo del rechazo");
        }
        SolicitudDevolucion solicitud = buscarPendiente(numeroSeguimiento);
        solicitud.setMotivoRechazo(motivo.trim());
        resolver(solicitud, EstadoDevolucion.RECHAZADA, rutAdmin);
        usuariosClient.enviarNotificacion(solicitud.getRutCliente(),
                "Su devolución #" + numeroSeguimiento + " fue rechazada. Motivo: " + motivo.trim(),
                "devolucion:" + numeroSeguimiento);
        registrar(rutAdmin, "Rechazó la devolución #" + numeroSeguimiento + ". Motivo: " + motivo.trim(),
                EstadoDevolucion.RECHAZADA);
        return devolucionRepository.save(solicitud);
    }

    /** ms-informes: devoluciones pendientes de revisión (HU-52). */
    public long contarPendientes() {
        return devolucionRepository.countByEstado(EstadoDevolucion.PENDIENTE);
    }

    // ---------- Apoyo ----------

    private SolicitudDevolucion findById(Long numeroSeguimiento) {
        return devolucionRepository.findById(numeroSeguimiento)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe la devolución #" + numeroSeguimiento));
    }

    private SolicitudDevolucion buscarPendiente(Long numeroSeguimiento) {
        SolicitudDevolucion solicitud = findById(numeroSeguimiento);
        if (solicitud.getEstado() != EstadoDevolucion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La devolución #" + numeroSeguimiento + " ya fue resuelta (" + solicitud.getEstado() + ")");
        }
        return solicitud;
    }

    private DetallePedido buscarDetalle(Pedido pedido, String sku) {
        return pedido.getDetalles().stream()
                .filter(d -> d.getSku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto " + sku + " no es parte del pedido #" + pedido.getNumeroPedido()));
    }

    /** Fecha en que el pedido pasó a ENTREGADO, tomada del historial. */
    private LocalDateTime fechaEntrega(Pedido pedido) {
        return pedido.getHistorial().stream()
                .filter(h -> h.getEstadoNuevo() == EstadoPedido.ENTREGADO)
                .map(HistorialEstadoPedido::getFecha)
                .reduce((primera, segunda) -> segunda)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El pedido no registra fecha de entrega"));
    }

    private void resolver(SolicitudDevolucion solicitud, EstadoDevolucion estado, String rutAdmin) {
        solicitud.setEstado(estado);
        solicitud.setRutAdministrador(rutAdmin.trim());
        solicitud.setFechaResolucion(LocalDateTime.now());
    }

    private void registrar(String rutAdmin, String descripcion, EstadoDevolucion nuevo) {
        RegistroAccionDTO accion = new RegistroAccionDTO();
        accion.setRutAdministrador(rutAdmin.trim());
        accion.setTipo("MODIFICAR");
        accion.setModulo(MODULO);
        accion.setDescripcion(descripcion);
        accion.getCambios().add(new DetalleCambioDTO("estado", EstadoDevolucion.PENDIENTE.name(), nuevo.name()));
        usuariosClient.registrarAccion(accion);
    }

    private String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
