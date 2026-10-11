package chibson.productos.repository;

import chibson.productos.model.EstadoResena;
import chibson.productos.model.Resena;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResenaRepository extends JpaRepository<Resena, Long> {

    List<Resena> findByProductoSkuAndEstadoOrderByFechaDesc(String sku, EstadoResena estado);

    List<Resena> findAllByOrderByFechaDesc();

    boolean existsByRutClienteAndProductoSku(String rutCliente, String sku);
}
