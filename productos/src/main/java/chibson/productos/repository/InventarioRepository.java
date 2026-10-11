package chibson.productos.repository;

import chibson.productos.model.Inventario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventarioRepository extends JpaRepository<Inventario, Long> {

    Optional<Inventario> findByProductoSku(String sku);

    List<Inventario> findByProductoSkuIn(List<String> skus);
}
