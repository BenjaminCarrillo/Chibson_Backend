package chibson.ventas.repository;

import chibson.ventas.model.EstadoDevolucion;
import chibson.ventas.model.SolicitudDevolucion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevolucionRepository extends JpaRepository<SolicitudDevolucion, Long> {

    List<SolicitudDevolucion> findByRutClienteOrderByFechaSolicitudDesc(String rutCliente);

    List<SolicitudDevolucion> findAllByOrderByFechaSolicitudDesc();

    long countByEstado(EstadoDevolucion estado);

    boolean existsByPedidoNumeroPedidoAndSkuAndEstadoNot(Long numeroPedido, String sku, EstadoDevolucion estado);
}
