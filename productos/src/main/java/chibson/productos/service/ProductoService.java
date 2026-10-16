package chibson.productos.service;

import chibson.productos.client.UsuariosClient;
import chibson.productos.client.VentasClient;
import chibson.productos.dto.DetalleCambioDTO;
import chibson.productos.dto.ProductoDTO;
import chibson.productos.dto.ProductoRespuestaDTO;
import chibson.productos.dto.RegistroAccionDTO;
import chibson.productos.model.Categoria;
import chibson.productos.model.EstadoProducto;
import chibson.productos.model.Producto;
import chibson.productos.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Módulo de catálogo (EP1): productos, categoría y ficha técnica. */
@Service
public class ProductoService {

    private static final String MODULO = "Productos";

    private final ProductoRepository productoRepository;
    private final UsuariosClient usuariosClient;
    private final VentasClient ventasClient;

    public ProductoService(ProductoRepository productoRepository, UsuariosClient usuariosClient,
                           VentasClient ventasClient) {
        this.productoRepository = productoRepository;
        this.usuariosClient = usuariosClient;
        this.ventasClient = ventasClient;
    }

    // ---------- Catálogo público ----------

    /** Solo productos activos (HU-03). */
    public List<ProductoRespuestaDTO> listarActivos() {
        return productoRepository.findByEstadoOrderByModeloAsc(EstadoProducto.ACTIVO).stream()
                .map(this::aRespuesta).toList();
    }

    /** Detalle público con stock y ficha técnica interpretada (HU-19). 404 si está inactivo. */
    public ProductoRespuestaDTO obtenerActivo(String sku) {
        Producto producto = productoRepository.findBySkuAndEstado(sku, EstadoProducto.ACTIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El producto no existe o no está disponible"));
        return aRespuesta(producto);
    }

    // ---------- Panel de administración ----------

    /** Listado de Productos.html con filtros opcionales por estado, categoría y SKU o modelo. */
    public List<ProductoRespuestaDTO> listarProductos(EstadoProducto estado, Categoria categoria, String texto) {
        String busqueda = (texto == null) ? "" : texto.trim().toLowerCase();
        return productoRepository.findAllByOrderByModeloAsc().stream()
                .filter(p -> estado == null || p.getEstado() == estado)
                .filter(p -> categoria == null || p.getCategoria() == categoria)
                .filter(p -> busqueda.isEmpty()
                        || p.getSku().toLowerCase().contains(busqueda)
                        || p.getModelo().toLowerCase().contains(busqueda))
                .map(this::aRespuesta).toList();
    }

    public Producto findBySku(String sku) {
        return productoRepository.findById(sku)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No existe el producto " + sku));
    }

    /** Crea el producto con SKU automático y su stock inicial (HU-01, HU-07). */
    @Transactional
    public ProductoRespuestaDTO crearProducto(ProductoDTO datos, String rutAdministrador) {
        validar(datos);
        validarNoNegativo(datos.getStockDisponible(), "stock inicial");
        int minimo = datos.getStockMinimo() == null ? StockService.STOCK_MINIMO_POR_DEFECTO : datos.getStockMinimo();
        validarNoNegativo(minimo, "stock mínimo");
        Producto producto = new Producto();
        producto.setSku(generarSku(datos.getCategoria()));
        copiarDatos(datos, producto);
        producto.setStockDisponible(datos.getStockDisponible());
        producto.setStockMinimo(minimo);
        producto.setEstado(Boolean.FALSE.equals(datos.getActivo()) ? EstadoProducto.INACTIVO : EstadoProducto.ACTIVO);
        producto.setFechaModificacion(LocalDateTime.now());
        Producto guardado = productoRepository.save(producto);

        registrar(rutAdministrador, "CREAR", "Creó el producto " + guardado.getModelo() + " (" + guardado.getSku()
                + ") con stock " + guardado.getStockDisponible(), new ArrayList<>());
        return aRespuesta(guardado);
    }

    /**
     * Edita los datos del producto y su stock (HU-02). Si el stock o el mínimo no vienen, se mantienen.
     * Los cambios de stock quedan en el historial junto con los demás campos (HU-73).
     */
    @Transactional
    public ProductoRespuestaDTO editarProducto(String sku, ProductoDTO datos, String rutAdministrador) {
        validar(datos);
        Producto producto = findBySku(sku);
        int stock = datos.getStockDisponible() == null ? producto.getStockDisponible() : datos.getStockDisponible();
        int minimo = datos.getStockMinimo() == null ? producto.getStockMinimo() : datos.getStockMinimo();
        validarNoNegativo(stock, "stock disponible");
        validarNoNegativo(minimo, "stock mínimo");
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        anotar(cambios, "stockDisponible", producto.getStockDisponible(), stock);
        anotar(cambios, "stockMinimo", producto.getStockMinimo(), minimo);
        producto.setStockDisponible(stock);
        producto.setStockMinimo(minimo);

        anotar(cambios, "modelo", producto.getModelo(), datos.getModelo().trim());
        anotar(cambios, "marca", producto.getMarca(), datos.getMarca().trim());
        anotar(cambios, "descripcion", producto.getDescripcion(), vacioANull(datos.getDescripcion()));
        anotar(cambios, "precio", producto.getPrecio(), datos.getPrecio());
        anotar(cambios, "categoria", producto.getCategoria(), datos.getCategoria());
        anotar(cambios, "imagenUrl", producto.getImagenUrl(), datos.getImagenUrl().trim());
        copiarDatos(datos, producto);
        producto.setFechaModificacion(LocalDateTime.now());
        Producto guardado = productoRepository.save(producto);

        registrar(rutAdministrador, "MODIFICAR", "Modificó el producto " + guardado.getModelo() + " (" + sku + ")", cambios);
        return aRespuesta(guardado);
    }

    /** Lo saca del catálogo público sin borrar su historial (HU-03). */
    public ProductoRespuestaDTO desactivarProducto(String sku, String rutAdministrador) {
        return cambiarEstado(sku, EstadoProducto.INACTIVO, "DESACTIVAR", "Desactivó", rutAdministrador);
    }

    public ProductoRespuestaDTO reactivarProducto(String sku, String rutAdministrador) {
        return cambiarEstado(sku, EstadoProducto.ACTIVO, "ACTIVAR", "Reactivó", rutAdministrador);
    }

    /** Solo se puede borrar si nunca estuvo en un pedido (HU-04). */
    @Transactional
    public void eliminarProducto(String sku, String rutAdministrador) {
        Producto producto = findBySku(sku);
        if (ventasClient.productoTienePedidos(sku)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El producto tiene pedidos asociados; desactívelo en lugar de eliminarlo");
        }
        productoRepository.delete(producto);
        registrar(rutAdministrador, "ELIMINAR", "Eliminó el producto " + producto.getModelo() + " (" + sku + ")",
                new ArrayList<>());
    }

    /** Agrega la ficha técnica a un producto que aún no la tiene (HU-05). */
    public ProductoRespuestaDTO agregarFichaTecnica(String sku, String texto, String rutAdministrador) {
        Producto producto = findBySku(sku);
        if (producto.getFichaTecnica() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto ya tiene ficha técnica; edítela en su lugar");
        }
        return guardarFicha(producto, texto, rutAdministrador, "Agregó");
    }

    /** Reemplaza el texto completo de la ficha técnica (HU-06). */
    public ProductoRespuestaDTO editarFichaTecnica(String sku, String texto, String rutAdministrador) {
        Producto producto = findBySku(sku);
        if (producto.getFichaTecnica() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto no tiene ficha técnica; agréguela primero");
        }
        return guardarFicha(producto, texto, rutAdministrador, "Editó");
    }

    /** Productos activos con stock igual o menor a su mínimo, del más urgente al menos urgente (HU-02, HU-50). */
    public List<ProductoRespuestaDTO> listarBajoStock() {
        return productoRepository.findAll().stream()
                .filter(p -> p.getEstado() == EstadoProducto.ACTIVO)
                .filter(Producto::esBajoStock)
                .sorted((a, b) -> Integer.compare(a.getStockDisponible(), b.getStockDisponible()))
                .map(this::aRespuesta)
                .toList();
    }

    /** Arma la respuesta con stock y ficha técnica interpretada. */
    public ProductoRespuestaDTO aRespuesta(Producto p) {
        return new ProductoRespuestaDTO(p.getSku(), p.getModelo(), p.getMarca(), p.getDescripcion(), p.getPrecio(),
                p.getCategoria(), p.getEstado(), p.getImagenUrl(), p.getFichaTecnica(),
                FichaTecnica.interpretar(p.getFichaTecnica()), p.getStockDisponible(), p.getStockMinimo(),
                p.esBajoStock(), p.estaAgotado(), p.getFechaModificacion(), p.getFechaModificacionFicha());
    }

    // ---------- Apoyo ----------

    /** SKU = prefijo de la categoría + correlativo de 4 dígitos, por ejemplo GTR-0007. */
    private String generarSku(Categoria categoria) {
        String prefijo = categoria.getPrefijo() + "-";
        int mayor = productoRepository.findBySkuStartingWith(prefijo).stream()
                .map(p -> p.getSku().substring(prefijo.length()))
                .filter(n -> n.chars().allMatch(Character::isDigit))
                .mapToInt(Integer::parseInt)
                .max().orElse(0);
        return prefijo + String.format("%04d", mayor + 1);
    }

    private ProductoRespuestaDTO guardarFicha(Producto producto, String texto, String rut, String verbo) {
        String limpio = FichaTecnica.validar(texto);
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        anotar(cambios, "fichaTecnica", producto.getFichaTecnica(), limpio);
        producto.setFichaTecnica(limpio);
        producto.setFechaModificacionFicha(LocalDateTime.now());
        Producto guardado = productoRepository.save(producto);
        registrar(rut, "MODIFICAR", verbo + " la ficha técnica de " + guardado.getModelo() + " (" + guardado.getSku() + ")",
                cambios);
        return aRespuesta(guardado);
    }

    private ProductoRespuestaDTO cambiarEstado(String sku, EstadoProducto nuevo, String tipo, String verbo, String rut) {
        Producto producto = findBySku(sku);
        if (producto.getEstado() == nuevo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto ya está " + nuevo.name().toLowerCase());
        }
        List<DetalleCambioDTO> cambios = new ArrayList<>();
        anotar(cambios, "estado", producto.getEstado(), nuevo);
        producto.setEstado(nuevo);
        producto.setFechaModificacion(LocalDateTime.now());
        Producto guardado = productoRepository.save(producto);
        registrar(rut, tipo, verbo + " el producto " + guardado.getModelo() + " (" + sku + ")", cambios);
        return aRespuesta(guardado);
    }

    private void validar(ProductoDTO datos) {
        obligatorio(datos.getModelo(), "El modelo es obligatorio");
        obligatorio(datos.getMarca(), "La marca es obligatoria");
        obligatorio(datos.getImagenUrl(), "La imagen es obligatoria");
        if (datos.getPrecio() == null || datos.getPrecio() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El precio debe ser mayor que cero");
        }
        if (datos.getCategoria() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría es obligatoria");
        }
    }

    private void copiarDatos(ProductoDTO datos, Producto producto) {
        producto.setModelo(datos.getModelo().trim());
        producto.setMarca(datos.getMarca().trim());
        producto.setDescripcion(vacioANull(datos.getDescripcion()));
        producto.setPrecio(datos.getPrecio());
        producto.setCategoria(datos.getCategoria());
        producto.setImagenUrl(datos.getImagenUrl().trim());
    }

    private void validarNoNegativo(Integer valor, String campo) {
        if (valor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El " + campo + " es obligatorio");
        }
        if (valor < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El " + campo + " no puede ser negativo");
        }
    }

    private void obligatorio(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
        }
    }

    private String vacioANull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor.trim();
    }

    private void anotar(List<DetalleCambioDTO> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(new DetalleCambioDTO(campo,
                    anterior == null ? null : anterior.toString(),
                    nuevo == null ? null : nuevo.toString()));
        }
    }

    private void registrar(String rut, String tipo, String descripcion, List<DetalleCambioDTO> cambios) {
        usuariosClient.registrarAccion(new RegistroAccionDTO(rut, tipo, MODULO, descripcion, cambios));
    }
}
