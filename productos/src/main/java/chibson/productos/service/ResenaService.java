package chibson.productos.service;

import chibson.productos.client.UsuariosClient;
import chibson.productos.client.VentasClient;
import chibson.productos.dto.DetalleCambioDTO;
import chibson.productos.dto.RegistroAccionDTO;
import chibson.productos.dto.ResenaDTO;
import chibson.productos.dto.ResenasProductoDTO;
import chibson.productos.model.EstadoProducto;
import chibson.productos.model.EstadoResena;
import chibson.productos.model.Producto;
import chibson.productos.model.Resena;
import chibson.productos.repository.ProductoRepository;
import chibson.productos.repository.ResenaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Módulo de reseñas (EP11). */
@Service
public class ResenaService {

    private static final String MODULO = "Reseñas";

    private final ResenaRepository resenaRepository;
    private final ProductoRepository productoRepository;
    private final UsuariosClient usuariosClient;
    private final VentasClient ventasClient;

    public ResenaService(ResenaRepository resenaRepository, ProductoRepository productoRepository,
                         UsuariosClient usuariosClient, VentasClient ventasClient) {
        this.resenaRepository = resenaRepository;
        this.productoRepository = productoRepository;
        this.usuariosClient = usuariosClient;
        this.ventasClient = ventasClient;
    }

    /**
     * El cliente califica un producto que compró y recibió (HU-81).
     * La reseña nace PENDIENTE y no es pública hasta que se aprueba (HU-86).
     */
    public Resena crearResena(ResenaDTO datos, String rutCliente) {
        String nombre = usuariosClient.obtenerNombre(rutCliente);
        String rut = rutCliente.trim().toUpperCase();
        Producto producto = productoRepository.findBySkuAndEstado(datos.getSku(), EstadoProducto.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe o no está disponible"));
        if (datos.getCalificacion() == null || datos.getCalificacion() < 1 || datos.getCalificacion() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La calificación debe estar entre 1 y 5 estrellas");
        }
        if (datos.getTitulo() == null || datos.getTitulo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El título es obligatorio");
        }
        if (resenaRepository.existsByRutClienteAndProductoSku(rut, producto.getSku())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya dejaste una reseña para este producto");
        }
        if (!ventasClient.clienteRecibioProducto(rut, producto.getSku())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Solo puedes reseñar productos que compraste y ya recibiste");
        }
        Resena resena = new Resena();
        resena.setProducto(producto);
        resena.setRutCliente(rut);
        resena.setNombreCliente(nombre);
        resena.setCalificacion(datos.getCalificacion());
        resena.setTitulo(datos.getTitulo().trim());
        resena.setComentario(datos.getComentario() == null || datos.getComentario().isBlank()
                ? null : datos.getComentario().trim());
        resena.setEstado(EstadoResena.PENDIENTE);
        resena.setFecha(LocalDateTime.now());
        return resenaRepository.save(resena);
    }

    /** Reseñas aprobadas de un producto, la más reciente primero, con promedio y total (HU-82). */
    public ResenasProductoDTO listarAprobadas(String sku) {
        List<Resena> aprobadas = resenaRepository.findByProductoSkuAndEstadoOrderByFechaDesc(sku, EstadoResena.APROBADA);
        double promedio = aprobadas.stream().mapToInt(Resena::getCalificacion).average().orElse(0);
        return new ResenasProductoDTO(sku, Math.round(promedio * 10) / 10.0, aprobadas.size(), aprobadas);
    }

    /**
     * Listado del administrador con filtros opcionales (HU-84).
     * Con el filtro de SKU sirve para "Ver reseñas" desde el panel de productos (HU-88).
     */
    public List<Resena> listarResenas(EstadoResena estado, String sku, Integer calificacion) {
        return resenaRepository.findAllByOrderByFechaDesc().stream()
                .filter(r -> estado == null || r.getEstado() == estado)
                .filter(r -> sku == null || sku.isBlank() || r.getSku().equalsIgnoreCase(sku.trim()))
                .filter(r -> calificacion == null || r.getCalificacion().equals(calificacion))
                .toList();
    }

    public Resena findById(Long id) {
        return resenaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe la reseña " + id));
    }

    /** Aprueba o rechaza una reseña pendiente (HU-86). */
    public Resena moderarResena(Long id, EstadoResena decision, String rutAdministrador) {
        if (decision != EstadoResena.APROBADA && decision != EstadoResena.RECHAZADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La decisión debe ser APROBADA o RECHAZADA");
        }
        Resena resena = findById(id);
        if (resena.getEstado() != EstadoResena.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden moderar reseñas pendientes");
        }
        String tipo = decision == EstadoResena.APROBADA ? "ACTIVAR" : "DESACTIVAR";
        String verbo = decision == EstadoResena.APROBADA ? "Aprobó" : "Rechazó";
        return cambiarEstado(resena, decision, tipo, verbo, rutAdministrador);
    }

    /** Retira de la vista pública una reseña ya aprobada, sin borrarla (HU-85). */
    public Resena ocultarResena(Long id, String rutAdministrador) {
        Resena resena = findById(id);
        if (resena.getEstado() != EstadoResena.APROBADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Solo se pueden ocultar reseñas aprobadas");
        }
        return cambiarEstado(resena, EstadoResena.OCULTA, "DESACTIVAR", "Ocultó", rutAdministrador);
    }

    /** Respuesta pública del administrador; si ya había una, se reemplaza (HU-87). */
    public Resena responderResena(Long id, String respuesta, String rutAdministrador) {
        if (respuesta == null || respuesta.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La respuesta no puede estar vacía");
        }
        Resena resena = findById(id);
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        cambios.add(new DetalleCambioDTO("respuestaAdmin", resena.getRespuestaAdmin(), respuesta.trim()));
        resena.setRespuestaAdmin(respuesta.trim());
        resena.setFechaRespuesta(LocalDateTime.now());
        Resena guardada = resenaRepository.save(resena);
        registrar(rutAdministrador, "MODIFICAR", "Respondió la reseña " + id + " de " + guardada.getModeloProducto(), cambios);
        return guardada;
    }

    /** Borrado definitivo; el promedio se recalcula solo porque se calcula al consultar (HU-83). */
    @Transactional
    public void eliminarResena(Long id, String rutAdministrador) {
        Resena resena = findById(id);
        String modelo = resena.getModeloProducto();
        resenaRepository.delete(resena);
        registrar(rutAdministrador, "ELIMINAR", "Eliminó la reseña " + id + " de " + modelo, new ArrayList<>());
    }

    private Resena cambiarEstado(Resena resena, EstadoResena nuevo, String tipo, String verbo, String rut) {
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        cambios.add(new DetalleCambioDTO("estado", resena.getEstado().name(), nuevo.name()));
        resena.setEstado(nuevo);
        Resena guardada = resenaRepository.save(resena);
        registrar(rut, tipo, verbo + " la reseña " + guardada.getIdResena() + " de " + guardada.getModeloProducto(), cambios);
        return guardada;
    }

    private void registrar(String rut, String tipo, String descripcion, List<DetalleCambioDTO> cambios) {
        usuariosClient.registrarAccion(new RegistroAccionDTO(rut, tipo, MODULO, descripcion, cambios));
    }
}
