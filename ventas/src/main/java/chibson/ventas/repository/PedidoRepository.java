package chibson.ventas.repository;

import chibson.ventas.model.EstadoPedido;
import chibson.ventas.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByRutClienteOrderByFechaDesc(String rutCliente);

    List<Pedido> findAllByOrderByFechaDesc();

    boolean existsByRutCliente(String rutCliente);

    boolean existsByDetallesSku(String sku);

    boolean existsByRutClienteAndEstadoAndDetallesSku(String rutCliente, EstadoPedido estado, String sku);
}
