package chibson.ventas.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import chibson.ventas.client.ProductosClient;
import chibson.ventas.client.UsuariosClient;
import chibson.ventas.dto.CheckoutDTO;
import chibson.ventas.dto.CompraRespuestaDTO;
import chibson.ventas.dto.DetalleCambioDTO;
import chibson.ventas.dto.DireccionDTO;
import chibson.ventas.dto.PedidoDTO;
import chibson.ventas.dto.PedidoResumenDTO;
import chibson.ventas.dto.ProductoDTO;
import chibson.ventas.dto.RegistroAccionDTO;
import chibson.ventas.dto.UsuarioDTO;
import chibson.ventas.model.Carrito;
import chibson.ventas.model.DetallePedido;
import chibson.ventas.model.EstadoPago;
import chibson.ventas.model.EstadoPedido;
import chibson.ventas.model.HistorialEstadoPedido;
import chibson.ventas.model.ItemCarrito;
import chibson.ventas.model.MetodoPago;
import chibson.ventas.model.Pago;
import chibson.ventas.model.Pedido;
import chibson.ventas.repository.PedidoRepository;

/** Compra (HU-23 a HU-25) y gestión de pedidos (HU-33 a HU-38, HU-44). Las notificaciones al cliente cubren HU-38. */
@Service
public class PedidoService {

    private static final String MODULO = "Pedidos";

    private final PedidoRepository pedidoRepository;
    private final CarritoService carritoService;
    private final ProductosClient productosClient;
    private final UsuariosClient usuariosClient;
    private final int costoEnvio;

    public PedidoService(PedidoRepository pedidoRepository, CarritoService carritoService,
                         ProductosClient productosClient, UsuariosClient usuariosClient,
                         @Value("${ventas.costo-envio}") int costoEnvio) {
        this.pedidoRepository = pedidoRepository;
        this.carritoService = carritoService;
        this.productosClient = productosClient;
        this.usuariosClient = usuariosClient;
        this.costoEnvio = costoEnvio;
    }

    // ---------- Compra (cliente) ----------

    /**
     * HU-23 y HU-25: crea el pedido con lo que hay en el carro y lo intenta pagar.
     * Si el pago se rechaza, el pedido queda PENDIENTE y el carro se mantiene para reintentar.
     */
    @Transactional
    public CompraRespuestaDTO comprar(String rutHeader, CheckoutDTO datos) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        if (datos.getMetodoPago() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe seleccionar un método de pago");
        }
        Carrito carrito = carritoService.obtener(cliente.getRut());
        if (carrito.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Su carro está vacío");
        }

        Pedido pedido = new Pedido();
        pedido.setRutCliente(cliente.getRut());
        pedido.setFecha(LocalDateTime.now());
        pedido.setEstado(EstadoPedido.PENDIENTE);
        pedido.setMetodoPago(datos.getMetodoPago());
        asignarDireccion(pedido, cliente, datos);

        // Precio y stock se vuelven a consultar: pudieron cambiar desde que se agregó al carro
        for (ItemCarrito item : carrito.getItems()) {
            ProductoDTO producto = productosClient.obtenerProducto(item.getSku());
            validarStock(producto, item.getCantidad());
            DetallePedido detalle = new DetallePedido();
            detalle.setSku(producto.getSku());
            detalle.setNombreProducto(producto.getModelo());
            detalle.setCategoria(producto.getCategoria());
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(producto.getPrecio());
            detalle.setPedido(pedido);
            pedido.getDetalles().add(detalle);
        }
        calcularMontos(pedido);
        agregarHistorial(pedido, null, EstadoPedido.PENDIENTE, cliente.getRut());
        pedido = pedidoRepository.save(pedido);

    return procesarPago(pedido, cliente, datos.getMetodoPago(), Boolean.TRUE.equals(datos.getSimularRechazo()));
    }

    /** HU-23 criterio 4: reintentar el pago de un pedido que quedó PENDIENTE. */
    @Transactional
    public CompraRespuestaDTO reintentarPago(Long numeroPedido, String rutHeader, MetodoPago metodo,
                                             boolean simularRechazo) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        Pedido pedido = buscarDelCliente(numeroPedido, cliente.getRut());
        if (pedido.getEstado() != EstadoPedido.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El pedido #" + numeroPedido + " no está pendiente de pago (estado " + pedido.getEstado() + ")");
        }
        if (metodo != null) {
            pedido.setMetodoPago(metodo);
        }
        return procesarPago(pedido, cliente, pedido.getMetodoPago(), simularRechazo);
    }

    /** Pasarela de pago simulada. */
    private CompraRespuestaDTO procesarPago(Pedido pedido, UsuarioDTO cliente, MetodoPago metodo,
                                            boolean simularRechazo) {
        Pago pago = new Pago();
        pago.setMetodo(metodo);
        pago.setMonto(pedido.getTotal());
        pago.setFecha(LocalDateTime.now());
        pago.setPedido(pedido);

        if (simularRechazo) {
            pago.setEstado(EstadoPago.RECHAZADO);
            pago.setMotivoRechazo("Fondos insuficientes (simulado)");
            pedido.getPagos().add(pago);
            pedido = pedidoRepository.save(pedido);
            return new CompraRespuestaDTO(false,
                    "El pago fue rechazado: " + pago.getMotivoRechazo() + ". Su carro se mantiene para reintentar",
                    aDTO(pedido, cliente));
        }

        descontarStock(pedido);
        pago.setEstado(EstadoPago.APROBADO);
        pedido.getPagos().add(pago);
        agregarHistorial(pedido, EstadoPedido.PENDIENTE, EstadoPedido.PAGADO, cliente.getRut());
        pedido.setEstado(EstadoPedido.PAGADO);
        pedido = pedidoRepository.save(pedido);

        carritoService.vaciar(cliente.getRut());
        usuariosClient.enviarNotificacion(cliente.getRut(),
                "Su compra fue aprobada. Pedido #" + pedido.getNumeroPedido() + " por $" + pedido.getTotal(),
                "pedido:" + pedido.getNumeroPedido());
        return new CompraRespuestaDTO(true, "Compra realizada con éxito", aDTO(pedido, cliente));
    }

    /**
     * Descuenta el stock línea por línea. Si una falla (por ejemplo, otro cliente compró
     * la última unidad), se repone lo ya descontado y se rechaza la compra.
     */
    private void descontarStock(Pedido pedido) {
        List<DetallePedido> descontados = new ArrayList<>();
        try {
            for (DetallePedido d : pedido.getDetalles()) {
                productosClient.descontarStock(d.getSku(), d.getCantidad(), pedido.getNumeroPedido());
                descontados.add(d);
            }
        } catch (ResponseStatusException e) {
            for (DetallePedido d : descontados) {
                try {
                    productosClient.reponerStock(d.getSku(), d.getCantidad(), pedido.getNumeroPedido(), "CANCELACION");
                } catch (ResponseStatusException ignorada) {
                    // Si tampoco se puede reponer, queda registrado en el log de ms-productos
                }
            }
            throw e;
        }
    }

    // ---------- Consultas del cliente ----------

    /** HU-33: pedidos del cliente, del más reciente al más antiguo. */
    @Transactional(readOnly = true)
    public List<PedidoDTO> listarPorCliente(String rutHeader) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        return pedidoRepository.findByRutClienteOrderByFechaDesc(cliente.getRut()).stream()
                .map(p -> aDTO(p, cliente))
                .toList();
    }

    /** HU-33 y HU-36: detalle e historial de un pedido propio. */
    @Transactional(readOnly = true)
    public PedidoDTO detalleCliente(Long numeroPedido, String rutHeader) {
        UsuarioDTO cliente = usuariosClient.obtenerCliente(rutHeader);
        return aDTO(buscarDelCliente(numeroPedido, cliente.getRut()), cliente);
    }

    // ---------- Administración ----------

    /** HU-34: todos los pedidos, del más reciente al más antiguo, con filtro opcional por estado. */
    @Transactional(readOnly = true)
    public List<PedidoDTO> listar(String rutAdmin, EstadoPedido estado) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        Map<String, UsuarioDTO> cache = new HashMap<>();
        return pedidoRepository.findAllByOrderByFechaDesc().stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .map(p -> aDTO(p, cache.computeIfAbsent(p.getRutCliente(), usuariosClient::buscarUsuario)))
                .toList();
    }

    @Transactional(readOnly = true)
    public PedidoDTO detalleAdmin(Long numeroPedido, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        Pedido pedido = findById(numeroPedido);
        return aDTO(pedido, usuariosClient.buscarUsuario(pedido.getRutCliente()));
    }

    /** HU-35 y HU-38: avanza el estado del pedido siguiendo el flujo y avisa al cliente. */
    @Transactional
    public PedidoDTO actualizarEstado(Long numeroPedido, EstadoPedido nuevo, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        if (nuevo == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El nuevo estado es obligatorio");
        }
        Pedido pedido = findById(numeroPedido);
        if (nuevo == EstadoPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Para cancelar use la opción Cancelar pedido, que pide un motivo");
        }
        if (!pedido.puedeCambiarA(nuevo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede pasar de " + pedido.getEstado() + " a " + nuevo
                            + ". El flujo es PAGADO → EN_PROCESO → ENVIADO → ENTREGADO");
        }
        EstadoPedido anterior = pedido.getEstado();
        agregarHistorial(pedido, anterior, nuevo, rutAdmin.trim());
        pedido.setEstado(nuevo);
        pedido = pedidoRepository.save(pedido);

        usuariosClient.enviarNotificacion(pedido.getRutCliente(),
                "Pedido #" + numeroPedido + ": nuevo estado " + nuevo, "pedido:" + numeroPedido);
        registrar(rutAdmin, "MODIFICAR", "Cambió el estado del pedido #" + numeroPedido,
                new DetalleCambioDTO("estado", anterior.name(), nuevo.name()));
        return aDTO(pedido, usuariosClient.buscarUsuario(pedido.getRutCliente()));
    }

    /** HU-37: cancela un pedido no entregado. Si estaba pagado, el stock vuelve al inventario. */
    @Transactional
    public PedidoDTO cancelar(Long numeroPedido, String motivo, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        if (motivo == null || motivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe indicar el motivo de la cancelación");
        }
        Pedido pedido = findById(numeroPedido);
        if (pedido.getEstado() == EstadoPedido.ENTREGADO || pedido.getEstado() == EstadoPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No se puede cancelar un pedido " + pedido.getEstado());
        }
        if (pedido.tienePagoAprobado()) {
            for (DetallePedido d : pedido.getDetalles()) {
                productosClient.reponerStock(d.getSku(), d.getCantidad(), numeroPedido, "CANCELACION");
            }
        }
        EstadoPedido anterior = pedido.getEstado();
        agregarHistorial(pedido, anterior, EstadoPedido.CANCELADO, rutAdmin.trim());
        pedido.setEstado(EstadoPedido.CANCELADO);
        pedido.setMotivoCancelacion(motivo.trim());
        pedido = pedidoRepository.save(pedido);

        usuariosClient.enviarNotificacion(pedido.getRutCliente(),
                "Pedido #" + numeroPedido + " cancelado. Motivo: " + motivo.trim(), "pedido:" + numeroPedido);
        registrar(rutAdmin, "MODIFICAR", "Canceló el pedido #" + numeroPedido + ". Motivo: " + motivo.trim(),
                new DetalleCambioDTO("estado", anterior.name(), EstadoPedido.CANCELADO.name()));
        return aDTO(pedido, usuariosClient.buscarUsuario(pedido.getRutCliente()));
    }

    /** HU-44: elimina un pedido. No se permite si ya fue pagado (facturado) o entregado. */
    @Transactional
    public void eliminar(Long numeroPedido, String rutAdmin) {
        usuariosClient.exigirPermiso(rutAdmin, Permisos.GESTIONAR_PEDIDOS);
        Pedido pedido = findById(numeroPedido);
        if (pedido.tienePagoAprobado() || pedido.getEstado() == EstadoPedido.ENTREGADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No se puede eliminar el pedido #" + numeroPedido + " porque ya fue facturado. Puede cancelarlo");
        }
        pedidoRepository.delete(pedido);
        registrar(rutAdmin, "ELIMINAR", "Eliminó el pedido #" + numeroPedido + " del cliente " + pedido.getRutCliente());
    }

    // ---------- Consultas de otros microservicios ----------

    /** ms-usuarios: no se puede eliminar un cliente con pedidos (HU-17). */
    public boolean clienteTienePedidos(String rut) {
        return pedidoRepository.existsByRutCliente(rut);
    }

    /** ms-productos: no se puede eliminar un producto que aparece en pedidos (HU-04). */
    public boolean productoTienePedidos(String sku) {
        return pedidoRepository.existsByDetallesSku(sku);
    }

    /** ms-productos: solo puede reseñar quien recibió el producto (HU-81). */
    public boolean clienteRecibioProducto(String rut, String sku) {
        return pedidoRepository.existsByRutClienteAndEstadoAndDetallesSku(rut, EstadoPedido.ENTREGADO, sku);
    }

    /**
     * ms-informes: pedidos creados entre las fechas indicadas (ambas opcionales), con la región
     * de envío ya resuelta. Si ms-usuarios no responde, la región de los pedidos sin dirección
     * alternativa queda vacía.
     */
    @Transactional(readOnly = true)
    public List<PedidoResumenDTO> resumen(LocalDate desde, LocalDate hasta) {
        Map<String, UsuarioDTO> cache = new HashMap<>();
        return pedidoRepository.findAllByOrderByFechaDesc().stream()
                .filter(p -> desde == null || !p.getFecha().toLocalDate().isBefore(desde))
                .filter(p -> hasta == null || !p.getFecha().toLocalDate().isAfter(hasta))
                .map(p -> {
                    String region = p.getRegionEnvio();
                    if (region == null) {
                        UsuarioDTO cliente = cache.computeIfAbsent(p.getRutCliente(), usuariosClient::buscarUsuario);
                        region = cliente == null ? null : cliente.getRegion();
                    }
                    return new PedidoResumenDTO(p.getNumeroPedido(), p.getFecha(), p.getEstado(), p.getTotal(),
                            region, p.tienePagoAprobado(), new ArrayList<>(p.getDetalles()));
                })
                .toList();
    }

    // ---------- Apoyo ----------

    public Pedido findById(Long numeroPedido) {
        return pedidoRepository.findById(numeroPedido)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No existe el pedido #" + numeroPedido));
    }

    /** Un cliente solo ve sus pedidos; si es de otro, responde como si no existiera. */
    public Pedido buscarDelCliente(Long numeroPedido, String rutCliente) {
        Pedido pedido = findById(numeroPedido);
        if (!pedido.getRutCliente().equals(rutCliente)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el pedido #" + numeroPedido);
        }
        return pedido;
    }

    /** HU-25: dirección alternativa solo para este pedido, o la del perfil. */
    private void asignarDireccion(Pedido pedido, UsuarioDTO cliente, CheckoutDTO datos) {
        if (Boolean.TRUE.equals(datos.getUsarDireccionDistinta())) {
            pedido.setRegionEnvio(obligatorio(datos.getRegion(), "la región"));
            pedido.setCiudadComunaEnvio(obligatorio(datos.getCiudadComuna(), "la ciudad o comuna"));
            pedido.setNumeroEnvio(obligatorio(datos.getNumero(), "el número"));
            pedido.setCondominioEnvio(vacioANull(datos.getCondominio()));
        } else if (vacio(cliente.getRegion()) || vacio(cliente.getCiudadComuna()) || vacio(cliente.getNumero())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Su perfil no tiene una dirección completa. Complétela o ingrese una dirección distinta para este pedido");
        }
    }

    /** Precios con IVA incluido: el IVA se informa, no se suma. El total agrega el envío. */
    private void calcularMontos(Pedido pedido) {
        int subtotal = pedido.getDetalles().stream().mapToInt(DetallePedido::subtotal).sum();
        pedido.setSubtotal(subtotal);
        pedido.setIva((int) Math.round(subtotal * 19 / 119.0));
        pedido.setCostoEnvio(costoEnvio);
        pedido.setTotal(subtotal + costoEnvio);
    }

    private void validarStock(ProductoDTO producto, int cantidad) {
        int stock = producto.getStockDisponible() == null ? 0 : producto.getStockDisponible();
        if (cantidad > stock) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, stock == 0
                    ? producto.getModelo() + " se agotó. Quítelo del carro para continuar"
                    : "Solo quedan " + stock + " unidades de " + producto.getModelo() + ". Ajuste la cantidad en su carro");
        }
    }

    private void agregarHistorial(Pedido pedido, EstadoPedido anterior, EstadoPedido nuevo, String rutResponsable) {
        HistorialEstadoPedido h = new HistorialEstadoPedido();
        h.setEstadoAnterior(anterior);
        h.setEstadoNuevo(nuevo);
        h.setRutResponsable(rutResponsable);
        h.setFecha(LocalDateTime.now());
        h.setPedido(pedido);
        pedido.getHistorial().add(h);
    }

    private void registrar(String rutAdmin, String tipo, String descripcion, DetalleCambioDTO... cambios) {
        RegistroAccionDTO accion = new RegistroAccionDTO();
        accion.setRutAdministrador(rutAdmin.trim());
        accion.setTipo(tipo);
        accion.setModulo(MODULO);
        accion.setDescripcion(descripcion);
        accion.setCambios(new ArrayList<>(List.of(cambios)));
        usuariosClient.registrarAccion(accion);
    }

    /** Arma la respuesta. cliente puede ser null si ms-usuarios no respondió en un listado. */
    public PedidoDTO aDTO(Pedido p, UsuarioDTO cliente) {
        DireccionDTO direccion;
        if (p.usaDireccionAlternativa()) {
            direccion = new DireccionDTO(p.getRegionEnvio(), p.getCiudadComunaEnvio(), p.getCondominioEnvio(),
                    p.getNumeroEnvio(), true);
        } else if (cliente != null) {
            direccion = new DireccionDTO(cliente.getRegion(), cliente.getCiudadComuna(), cliente.getCondominio(),
                    cliente.getNumero(), false);
        } else {
            direccion = null;
        }
        return new PedidoDTO(p.getNumeroPedido(), p.getRutCliente(),
                cliente == null ? null : cliente.nombreCompleto(), p.getFecha(), p.getEstado(),
                p.getSubtotal(), p.getCostoEnvio(), p.getIva(), p.getTotal(), p.getMetodoPago(), direccion,
                p.getMotivoCancelacion(), new ArrayList<>(p.getDetalles()), new ArrayList<>(p.getHistorial()),
                new ArrayList<>(p.getPagos()));
    }

    private String obligatorio(String valor, String campo) {
        if (vacio(valor)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe ingresar " + campo + " de envío");
        }
        return valor.trim();
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }

    private String vacioANull(String valor) {
        return vacio(valor) ? null : valor.trim();
    }
}
