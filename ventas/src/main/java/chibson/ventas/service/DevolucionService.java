package chibson.ventas.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.client.ProductosClient;
import chibson.ventas.client.UsuariosClient;
import chibson.ventas.dto.DetalleCambioDTO;
import chibson.ventas.dto.DevolucionDTO;
import chibson.ventas.dto.ItemDTO;
import chibson.ventas.dto.RegistroAccionDTO;
import chibson.ventas.dto.UsuarioDTO;
import chibson.ventas.model.ItemPedido;
import chibson.ventas.model.EstadoDevolucion;
import chibson.ventas.model.EstadoPedido;
import chibson.ventas.model.HistorialEstadoPedido;
import chibson.ventas.model.ItemDevolucion;
import chibson.ventas.model.Pedido;
import chibson.ventas.model.SolicitudDevolucion;
import chibson.ventas.repository.DevolucionRepository;

/** Devoluciones (HU-39 a HU-43) y su reembolso (HU-89). */
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

    /**
     * HU-39: el cliente solicita devolver uno o varios productos de un pedido entregado, indicando
     * cuántas unidades de cada uno. No puede devolver más de lo que compró, contando las solicitudes
     * anteriores del mismo pedido que no fueron rechazadas.
     */
    @Transactional
    public SolicitudDevolucion solicitar(String rutHeader, DevolucionDTO datos) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        if (datos.getNumeroPedido() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El número de pedido es obligatorio");
        }
        if (datos.getMotivo() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un motivo");
        }
        Map<String, Integer> pedidas = agruparItems(datos.getItems());
        Pedido pedido = pedidoService.buscarDelCliente(datos.getNumeroPedido(), cliente.getRut());

        if (pedido.getEstado() != EstadoPedido.ENTREGADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se pueden devolver productos de pedidos entregados");
        }
        LocalDateTime entrega = fechaEntrega(pedido);
        if (entrega.plusDays(DIAS_PLAZO).isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El plazo de " + DIAS_PLAZO + " días para solicitar la devolución ya venció");
        }

        Map<String, Integer> yaSolicitadas = unidadesYaSolicitadas(pedido.getNumeroPedido());
        SolicitudDevolucion solicitud = new SolicitudDevolucion();
        for (Map.Entry<String, Integer> pedida : pedidas.entrySet()) {
            String sku = pedida.getKey();
            int cantidad = pedida.getValue();
            ItemPedido comprado = buscarItem(pedido, sku);
            int anteriores = yaSolicitadas.getOrDefault(sku, 0);
            int disponibles = comprado.getCantidad() - anteriores;
            if (disponibles <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Ya solicitó la devolución de todas las unidades de " + comprado.getNombreProducto()
                                + " (" + sku + ") de este pedido");
            }
            if (cantidad > disponibles) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "De " + comprado.getNombreProducto() + " (" + sku + ") compró " + comprado.getCantidad()
                                + " unidad(es)" + (anteriores > 0 ? " y ya tiene " + anteriores + " en otras solicitudes" : "")
                                + ": puede devolver como máximo " + disponibles);
            }
            ItemDevolucion item = new ItemDevolucion();
            item.setSku(sku);
            item.setNombreProducto(comprado.getNombreProducto());
            item.setCantidad(cantidad);
            item.setPrecioUnitario(comprado.getPrecioUnitario());
            item.setSolicitud(solicitud);
            solicitud.getItems().add(item);
        }

        solicitud.setRutCliente(cliente.getRut());
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

    /** HU-41: aprueba la devolución y repone el stock solo de los productos y cantidades de la solicitud. */
    @Transactional
    public SolicitudDevolucion aprobar(Long numeroSeguimiento, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        SolicitudDevolucion solicitud = buscarPendiente(numeroSeguimiento);
        for (ItemDevolucion item : solicitud.getItems()) {
            productosClient.reponerStock(item.getSku(), item.getCantidad(), solicitud.getNumeroPedido(),
                    "DEVOLUCION", rutAdmin.trim());
        }
        resolver(solicitud, EstadoDevolucion.APROBADA, rutAdmin);
        usuariosClient.enviarNotificacion(solicitud.getRutCliente(),
                "Su devolución #" + numeroSeguimiento + " fue aprobada. Recibirá un reembolso de "
                        + pesos(solicitud.calcularMontoReembolso()) + "; le avisaremos cuando se realice",
                "devolucion:" + numeroSeguimiento);
        registrar(rutAdmin, "Aprobó la devolución #" + numeroSeguimiento + " del pedido #" + solicitud.getNumeroPedido(),
                new DetalleCambioDTO("estado", EstadoDevolucion.PENDIENTE.name(), EstadoDevolucion.APROBADA.name()));
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
                new DetalleCambioDTO("estado", EstadoDevolucion.PENDIENTE.name(), EstadoDevolucion.RECHAZADA.name()));
        return devolucionRepository.save(solicitud);
    }

    /**
     * HU-89: marca como reembolsada una devolución aprobada. El monto se calcula con las unidades
     * devueltas y el precio que el cliente pagó. Después ya no puede cambiar de estado.
     */
    @Transactional
    public SolicitudDevolucion reembolsar(Long numeroSeguimiento, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        SolicitudDevolucion solicitud = findById(numeroSeguimiento);
        if (solicitud.getEstado() == EstadoDevolucion.REEMBOLSADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La devolución #" + numeroSeguimiento + " ya fue reembolsada");
        }
        if (solicitud.getEstado() != EstadoDevolucion.APROBADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se puede reembolsar una devolución aprobada; la #" + numeroSeguimiento
                            + " está " + solicitud.getEstado());
        }
        solicitud.reembolsar(rutAdmin.trim());
        usuariosClient.enviarNotificacion(solicitud.getRutCliente(),
                "Su devolución #" + numeroSeguimiento + " fue reembolsada por " + pesos(solicitud.getMontoReembolsado()),
                "devolucion:" + numeroSeguimiento);
        registrar(rutAdmin, "Reembolsó la devolución #" + numeroSeguimiento + " del pedido #" + solicitud.getNumeroPedido()
                        + " por " + pesos(solicitud.getMontoReembolsado()),
                new DetalleCambioDTO("estado", EstadoDevolucion.APROBADA.name(), EstadoDevolucion.REEMBOLSADA.name()),
                new DetalleCambioDTO("montoReembolsado", "", String.valueOf(solicitud.getMontoReembolsado())));
        return devolucionRepository.save(solicitud);
    }

    /** ms-informes: devoluciones pendientes de revisión para el resumen general (HU-50). */
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

    private ItemPedido buscarItem(Pedido pedido, String sku) {
        return pedido.getItems().stream()
                .filter(i -> i.getSku().equals(sku))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El producto " + sku + " no es parte del pedido #" + pedido.getNumeroPedido()));
    }

    /** Valida los productos de la solicitud y suma las cantidades si un SKU viene repetido. */
    private Map<String, Integer> agruparItems(List<ItemDTO> items) {
        if (items == null || items.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe elegir al menos un producto a devolver");
        }
        Map<String, Integer> cantidades = new LinkedHashMap<>();
        for (ItemDTO item : items) {
            if (item.getSku() == null || item.getSku().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada producto debe indicar su SKU");
            }
            if (item.getCantidad() == null || item.getCantidad() < 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad a devolver debe ser al menos 1");
            }
            cantidades.merge(item.getSku().trim().toUpperCase(), item.getCantidad(), Integer::sum);
        }
        return cantidades;
    }

    /** Unidades de cada SKU que ya están en solicitudes no rechazadas de este pedido. */
    private Map<String, Integer> unidadesYaSolicitadas(Long numeroPedido) {
        Map<String, Integer> unidades = new HashMap<>();
        for (SolicitudDevolucion anterior : devolucionRepository.findByPedidoNumeroPedidoAndEstadoNot(
                numeroPedido, EstadoDevolucion.RECHAZADA)) {
            for (ItemDevolucion item : anterior.getItems()) {
                unidades.merge(item.getSku(), item.getCantidad(), Integer::sum);
            }
        }
        return unidades;
    }

    /** Monto en formato chileno: $1.450.000 */
    private String pesos(int monto) {
        return "$" + String.format(Locale.ROOT, "%,d", monto).replace(',', '.');
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

    private void registrar(String rutAdmin, String descripcion, DetalleCambioDTO... cambios) {
        RegistroAccionDTO accion = new RegistroAccionDTO();
        accion.setRutAdministrador(rutAdmin.trim());
        accion.setTipo("MODIFICAR");
        accion.setModulo(MODULO);
        accion.setDescripcion(descripcion);
        accion.getCambios().addAll(List.of(cambios));
        usuariosClient.registrarAccion(accion);
    }

    private String vacioANull(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
