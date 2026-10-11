package chibson.productos.repository;

import chibson.productos.model.EstadoProducto;
import chibson.productos.model.Producto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, String> {

    List<Producto> findByEstadoOrderByModeloAsc(EstadoProducto estado);

    Optional<Producto> findBySkuAndEstado(String sku, EstadoProducto estado);

    List<Producto> findAllByOrderByModeloAsc();

    /** Para calcular el siguiente correlativo del SKU. */
    List<Producto> findBySkuStartingWith(String prefijo);
}
